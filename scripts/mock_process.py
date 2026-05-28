import argparse
import json
import os
from datetime import datetime, timedelta
from pathlib import Path

os.environ.setdefault("MPLCONFIGDIR", str(Path(__file__).resolve().parents[1] / ".matplotlib-cache"))

import matplotlib

matplotlib.use("Agg")

import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
import xarray as xr
from matplotlib.colors import BoundaryNorm, ListedColormap

try:
    import cartopy.crs as ccrs
except ImportError:
    ccrs = None


def parse_mask_range(mask_range: str) -> tuple[float, float]:
    parts = [item.strip() for item in mask_range.split(",")]
    if len(parts) != 2:
        raise ValueError("mask_range must be formatted as min,max")
    mask_min = float(parts[0])
    mask_max = float(parts[1])
    if mask_min >= mask_max:
        raise ValueError("mask_range min must be less than max")
    return mask_min, mask_max


def build_evaluation_frame(target_date: datetime, seed: int) -> pd.DataFrame:
    rng = np.random.default_rng(seed)

    # Acceptance rule: the evaluation window is fixed to 8 days from target_date.
    dates = [target_date + timedelta(days=offset) for offset in range(8)]
    x = np.arange(len(dates))
    cloud_score = np.clip(0.32 + 0.05 * x + rng.normal(0, 0.015, len(dates)), 0, 1)
    detection_accuracy = np.clip(0.78 + 0.018 * x + rng.normal(0, 0.01, len(dates)), 0, 1)
    anomaly_mean = -0.8 + 0.22 * x + rng.normal(0, 0.08, len(dates))
    valid_pixels = rng.integers(8400, 9800, len(dates))

    df = pd.DataFrame(
        {
            "date": [date.strftime("%Y-%m-%d") for date in dates],
            "cloud_score": np.round(cloud_score, 4),
            "detection_accuracy": np.round(detection_accuracy, 4),
            "anomaly_mean": np.round(anomaly_mean, 4),
            "valid_pixels": valid_pixels,
        }
    )

    # Acceptance rule: all Average values are appended as the final row, never as a column.
    average_row = {"date": "Average"}
    for column in ["cloud_score", "detection_accuracy", "anomaly_mean", "valid_pixels"]:
        average_row[column] = round(float(df[column].mean()), 4)
    return pd.concat([df, pd.DataFrame([average_row])], ignore_index=True)


def draw_trend_chart(df: pd.DataFrame, output_path: Path) -> float:
    data = df[df["date"] != "Average"].copy()
    x = np.arange(len(data))
    accuracy = data["detection_accuracy"].astype(float).to_numpy()
    anomaly = data["anomaly_mean"].astype(float).to_numpy()
    slope = float(np.polyfit(x, accuracy, 1)[0])

    fig, ax1 = plt.subplots(figsize=(11.5, 5.6), dpi=160)
    ax1.plot(x, accuracy, color="#2563eb", linewidth=2.5, marker="o")
    ax1.set_ylim(0.72, 1.0)
    ax1.set_ylabel("Detection Accuracy")
    tick_step = 2 if len(data) <= 14 else 7
    tick_positions = list(range(0, len(data), tick_step))
    if tick_positions[-1] != len(data) - 1:
        tick_positions.append(len(data) - 1)
    ax1.set_xticks(tick_positions)
    ax1.set_xticklabels(data["date"].iloc[tick_positions], rotation=24, ha="right")
    ax1.set_xlim(-0.35, x[-1] + 1.85)
    ax1.grid(True, axis="y", linestyle="--", alpha=0.28)

    ax2 = ax1.twinx()
    ax2.plot(x, anomaly, color="#dc2626", linewidth=2.3, marker="s")
    ax2.set_ylabel("Anomaly Mean")

    # Acceptance rule: labels are placed at the top or right side; no legend is used.
    ax1.text(
        x[-1] + 0.42,
        accuracy[-1],
        "Detection Accuracy",
        color="#2563eb",
        va="center",
        fontsize=9,
    )
    ax2.text(
        x[-1] + 0.42,
        anomaly[-1],
        "Anomaly Mean",
        color="#dc2626",
        va="center",
        fontsize=9,
    )
    ax1.text(
        0.99,
        1.03,
        f"Slope: {slope:.2e}",
        transform=ax1.transAxes,
        ha="right",
        va="bottom",
        fontsize=10,
        fontweight="bold",
    )
    ax1.set_title("8-Day Vision Model Evaluation Trend", pad=18)
    fig.subplots_adjust(left=0.08, right=0.78, bottom=0.18, top=0.86)
    fig.savefig(output_path, bbox_inches="tight")
    plt.close(fig)
    return slope


def build_white_mask_cmap(vmin: float, vmax: float, mask_min: float, mask_max: float):
    bins = 256
    boundaries = np.linspace(vmin, vmax, bins + 1)
    centers = (boundaries[:-1] + boundaries[1:]) / 2
    colors = plt.get_cmap("coolwarm", bins)(np.linspace(0, 1, bins))
    colors[(centers >= mask_min) & (centers <= mask_max)] = np.array([1, 1, 1, 1])
    cmap = ListedColormap(colors)
    norm = BoundaryNorm(boundaries, cmap.N)
    return cmap, norm


def draw_anomaly_map(mask_min: float, mask_max: float, seed: int, output_path: Path) -> None:
    rng = np.random.default_rng(seed + 2026)
    lat = np.linspace(18, 54, 80)
    lon = np.linspace(73, 135, 120)
    lon_grid, lat_grid = np.meshgrid(lon, lat)
    field = (
        2.8 * np.sin((lon_grid - 90) / 9)
        + 1.7 * np.cos((lat_grid - 32) / 6)
        + rng.normal(0, 0.45, lon_grid.shape)
    )
    anomaly = xr.DataArray(field, coords={"lat": lat, "lon": lon}, dims=("lat", "lon"))

    vmin = min(-5.0, mask_min - 1.0)
    vmax = max(5.0, mask_max + 1.0)
    cmap, norm = build_white_mask_cmap(vmin, vmax, mask_min, mask_max)

    if ccrs is not None:
        fig = plt.figure(figsize=(10.2, 5.8), dpi=160)
        ax = plt.axes(projection=ccrs.PlateCarree())
        mesh = ax.pcolormesh(
            anomaly["lon"],
            anomaly["lat"],
            anomaly,
            transform=ccrs.PlateCarree(),
            cmap=cmap,
            norm=norm,
            shading="auto",
        )
        ax.gridlines(linewidth=0.25, color="#64748b", alpha=0.35, linestyle="--")
        ax.set_extent([73, 135, 18, 54], crs=ccrs.PlateCarree())
    else:
        fig, ax = plt.subplots(figsize=(10.2, 5.8), dpi=160)
        mesh = ax.pcolormesh(anomaly["lon"], anomaly["lat"], anomaly, cmap=cmap, norm=norm, shading="auto")
        ax.set_xlim(73, 135)
        ax.set_ylim(18, 54)

    ax.set_title("Spatial Anomaly Map with Pure White Noise Mask", pad=14)
    ax.text(
        0.02,
        0.98,
        f"White mask: [{mask_min:g}, {mask_max:g}]",
        transform=ax.transAxes,
        ha="left",
        va="top",
        fontsize=9,
        bbox={"boxstyle": "round,pad=0.32", "facecolor": "white", "edgecolor": "#cbd5e1", "alpha": 0.92},
    )
    colorbar = fig.colorbar(mesh, ax=ax, fraction=0.04, pad=0.035)
    colorbar.set_label("Anomaly Value")
    colorbar.set_ticks([vmin, mask_min, mask_max, vmax])
    fig.subplots_adjust(left=0.07, right=0.88, bottom=0.08, top=0.9)
    fig.savefig(output_path, bbox_inches="tight")
    plt.close(fig)


def write_manifest(
    task_id: str,
    target_date: datetime,
    output_dir: Path,
    mask_min: float,
    mask_max: float,
    slope: float,
) -> None:
    manifest = {
        "task_id": task_id,
        "target_date": target_date.strftime("%Y-%m-%d"),
        "window_start": target_date.strftime("%Y-%m-%d"),
        "window_end": (target_date + timedelta(days=7)).strftime("%Y-%m-%d"),
        "fixed_window_days": 8,
        "average_rule": "Average values are stored in the final row of evaluation_matrix.csv.",
        "label_rule": "Chart labels are placed at the top or right side; no legend is used.",
        "slope_scientific_notation": f"{slope:.2e}",
        "white_mask_range": [mask_min, mask_max],
        "outputs": [
            "evaluation_matrix.csv",
            "trend_chart.png",
            "anomaly_map.png",
        ],
    }
    (output_dir / "manifest.json").write_text(json.dumps(manifest, indent=2), encoding="utf-8")


def main() -> None:
    parser = argparse.ArgumentParser(description="Mock vision MLOps evaluation script")
    parser.add_argument("--task_id", required=True)
    parser.add_argument("--target_date", required=True)
    parser.add_argument("--mask_range", required=True)
    parser.add_argument("--output_root", default="../output")
    args = parser.parse_args()

    target_date = datetime.strptime(args.target_date, "%Y-%m-%d")
    mask_min, mask_max = parse_mask_range(args.mask_range)
    output_dir = Path(args.output_root).resolve() / str(args.task_id)
    output_dir.mkdir(parents=True, exist_ok=True)
    seed = abs(hash((args.task_id, args.target_date))) % (2**32)

    frame = build_evaluation_frame(target_date, seed)
    csv_path = output_dir / "evaluation_matrix.csv"
    frame.to_csv(csv_path, index=False)

    slope = draw_trend_chart(frame, output_dir / "trend_chart.png")
    draw_anomaly_map(mask_min, mask_max, seed, output_dir / "anomaly_map.png")
    write_manifest(args.task_id, target_date, output_dir, mask_min, mask_max, slope)

    print(f"Task {args.task_id} completed")
    print(f"CSV: {csv_path}")
    print(f"Trend chart: {output_dir / 'trend_chart.png'}")
    print(f"Anomaly map: {output_dir / 'anomaly_map.png'}")


if __name__ == "__main__":
    main()
