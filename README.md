# Java Vision MLOps Platform

基于 Java Spring Boot + Vue 3 的轻量级视觉模型与遥感数据 MLOps 调度平台。系统通过 Web 表单创建评估任务，Java 后端负责生成 Task ID、调用 Python 脚本、监控执行状态，并把 CSV/PNG 结果路径写入 MySQL，前端展示任务状态、CSV 表格和图像结果。

## 技术栈

- Backend: Java 17, Spring Boot 3.x, Spring Data JPA, MySQL 8.0
- Frontend: Vue 3, Vite, Element Plus
- Script: Python 3, pandas, matplotlib, numpy, xarray, cartopy

## 目录结构

```text
backend/               Spring Boot API
frontend/              Vue 3 management UI
scripts/mock_process.py Python evaluation mock script
database/init.sql      MySQL initialization script
output/                Generated task outputs
```

## 强制验收规则

`scripts/mock_process.py` 已经把以下规则写死：

1. 时间窗口固定为从目标日期开始的 8 天。
2. CSV 所有 Average 结果只追加在最后一行，不放在最后一列。
3. Matplotlib 图表标签只放顶部或右侧，代码中不使用 legend。
4. Slope 使用科学记数法，例如 `1.82e-02`。
5. Anomaly map 使用前端传入区间生成纯白遮罩，Colorbar 中对应区间同样显示为纯白。

## 数据库初始化

最快演示可以直接使用一键脚本，脚本会优先尝试连接 MySQL；如果 MySQL 不可用，会自动切换到内置 H2 文件数据库，避免演示被数据库安装卡住。

```powershell
.\start-dev.bat
```

脚本会自动执行：

1. 创建 Python 虚拟环境 `.venv`。
2. 安装 Python 依赖。
3. 安装前端 npm 依赖。
4. 执行 Python mock 脚本烟测。
5. 打包 Spring Boot 后端。
6. 构建 Vue 前端。
7. 启动后端 `http://localhost:8080` 和前端 `http://localhost:5173`。

如果要强制使用 MySQL：

```powershell
.\start-dev.bat -Database mysql -DbUser root -DbPassword 123456
```

如果只想安装依赖并做构建自检，不启动服务：

```powershell
.\start-dev.bat -PrepareOnly -Database h2
```

如果依赖已经装好，想快速启动：

```powershell
.\start-dev.bat -SkipInstall -NoBuild -Database h2
```

系统级前置环境：Java 17+、Maven、Node.js、Python 3。MySQL 8.0 是正式交付数据库；无 MySQL 时脚本会用 H2 demo 模式跑通全流程。

### MySQL 手动初始化

```bash
mysql -u root -p < database/init.sql
```

默认后端配置位于 `backend/src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/mlops_platform?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: 123456
```

如本地 MySQL 密码不同，修改 `password` 即可。

## Python 环境

```bash
python -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r scripts\requirements.txt
```

也可以单独验证脚本：

```bash
.\.venv\Scripts\python.exe scripts\mock_process.py --task_id 1001 --target_date 2026-05-19 --mask_range=-1,1 --output_root output
```

执行完成后会生成：

```text
output/1001/evaluation_matrix.csv
output/1001/trend_chart.png
output/1001/anomaly_map.png
output/1001/manifest.json
```

## 启动后端

```bash
cd backend
mvn spring-boot:run
```

后端地址：

```text
http://localhost:8080
```

主要接口：

```text
POST /api/tasks
GET  /api/tasks
GET  /api/tasks/{id}
GET  /api/tasks/{id}/csv
GET  /api/tasks/{id}/images
GET  /api/assets
POST /api/assets
```

## 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端地址：

```text
http://localhost:5173
```

## 演示流程

1. 打开前端页面。
2. 点击“新建任务”。
3. 输入测试名称、目标日期、白色遮罩区间，例如 `-1` 到 `1`。
4. 提交后任务状态进入 `RUNNING`。
5. Python 脚本执行完成后，任务状态变为 `SUCCESS`。
6. 点击“结果”查看 CSV 数据矩阵、趋势图和空间异常图。

## 论文可写模块

- Model Asset Registry: `model_asset` 表记录脚本资产与路径。
- Evaluation Scheduler: Java 使用 `ProcessBuilder` 调度 Python 脚本。
- Task State Tracking: `evaluation_task` 表记录 Running/Success/Failed 状态。
- Result Lineage: `task_result` 表保存 CSV、PNG 结果路径。
- Automated Evaluation: Python 脚本固定执行 8 天窗口、末行平均值、科学记数法斜率和白色噪声遮罩。
