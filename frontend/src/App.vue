<template>
  <el-container class="app-shell">
    <el-header class="topbar">
      <div>
        <h1>视觉模型与遥感数据 MLOps 调度平台</h1>
        <p>Vision Model Lifecycle & Automated Evaluation</p>
      </div>
      <el-button :icon="Refresh" @click="refreshAll" :loading="loading">刷新</el-button>
    </el-header>

    <el-main class="main">
      <el-tabs v-model="activeTab" class="workspace-tabs">
        <el-tab-pane label="任务调度" name="tasks">
          <section class="toolbar">
            <el-button type="primary" :icon="Plus" @click="taskDialogVisible = true">新建任务</el-button>
          </section>

          <el-table :data="tasks" class="data-table" stripe>
            <el-table-column prop="id" label="Task ID" width="92" />
            <el-table-column prop="taskName" label="测试名称" min-width="180" />
            <el-table-column prop="targetDate" label="目标日期" width="130" />
            <el-table-column label="白色遮罩区间" width="150">
              <template #default="{ row }">[{{ row.maskMin }}, {{ row.maskMax }}]</template>
            </el-table-column>
            <el-table-column label="状态" width="120">
              <template #default="{ row }">
                <el-tag :type="statusType(row.status)" effect="light">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="创建时间" width="190" />
            <el-table-column label="操作" width="132" fixed="right">
              <template #default="{ row }">
                <el-button :icon="View" size="small" @click="openResult(row)" :disabled="row.status !== 'SUCCESS'">
                  结果
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="模型资产" name="assets">
          <section class="toolbar">
            <el-button type="primary" :icon="Plus" @click="assetDialogVisible = true">登记脚本</el-button>
          </section>
          <el-table :data="assets" class="data-table" stripe>
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="assetName" label="资产名称" min-width="220" />
            <el-table-column prop="scriptPath" label="脚本路径" min-width="260" />
            <el-table-column prop="description" label="说明" min-width="280" />
            <el-table-column prop="createdAt" label="创建时间" width="190" />
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-main>
  </el-container>

  <el-dialog v-model="taskDialogVisible" title="新建评估任务" width="520px">
    <el-form :model="taskForm" label-width="110px">
      <el-form-item label="测试名称">
        <el-input v-model="taskForm.taskName" maxlength="64" />
      </el-form-item>
      <el-form-item label="目标日期">
        <el-date-picker
          v-model="taskForm.targetDate"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="选择日期"
          class="full-width"
        />
      </el-form-item>
      <el-form-item label="遮罩最小值">
        <el-input-number v-model="taskForm.maskMin" :precision="2" :step="0.5" class="full-width" />
      </el-form-item>
      <el-form-item label="遮罩最大值">
        <el-input-number v-model="taskForm.maskMax" :precision="2" :step="0.5" class="full-width" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="taskDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="submitTask">提交</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="assetDialogVisible" title="登记模型脚本资产" width="560px">
    <el-form :model="assetForm" label-width="100px">
      <el-form-item label="资产名称">
        <el-input v-model="assetForm.assetName" maxlength="80" />
      </el-form-item>
      <el-form-item label="脚本路径">
        <el-input v-model="assetForm.scriptPath" />
      </el-form-item>
      <el-form-item label="说明">
        <el-input v-model="assetForm.description" type="textarea" :rows="3" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="assetDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="submitAsset">保存</el-button>
    </template>
  </el-dialog>

  <el-drawer v-model="resultVisible" size="78%" title="评估结果">
    <template v-if="selectedTask">
      <div class="result-header">
        <div>
          <span class="muted">Task ID</span>
          <strong>{{ selectedTask.id }}</strong>
        </div>
        <div>
          <span class="muted">目标日期</span>
          <strong>{{ selectedTask.targetDate }}</strong>
        </div>
        <div>
          <span class="muted">遮罩区间</span>
          <strong>[{{ selectedTask.maskMin }}, {{ selectedTask.maskMax }}]</strong>
        </div>
      </div>

      <h2>CSV 数据矩阵</h2>
      <el-table :data="csv.rows" border class="data-table csv-table" max-height="360">
        <el-table-column
          v-for="header in csv.headers"
          :key="header"
          :prop="header"
          :label="header"
          min-width="138"
        />
      </el-table>

      <h2>图像结果</h2>
      <div class="image-grid">
        <figure v-for="image in images" :key="image.type">
          <figcaption>{{ image.name }}</figcaption>
          <img :src="`${image.url}?t=${cacheBust}`" :alt="image.name" />
        </figure>
      </div>
    </template>
  </el-drawer>
</template>

<script setup>
import { onMounted, onUnmounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Refresh, View } from '@element-plus/icons-vue'
import {
  createAsset,
  createTask,
  getTaskCsv,
  getTaskImages,
  listAssets,
  listTasks
} from './api'

const activeTab = ref('tasks')
const loading = ref(false)
const submitting = ref(false)
const tasks = ref([])
const assets = ref([])
const csv = reactive({ headers: [], rows: [] })
const images = ref([])
const selectedTask = ref(null)
const resultVisible = ref(false)
const taskDialogVisible = ref(false)
const assetDialogVisible = ref(false)
const cacheBust = ref(Date.now())
let pollTimer = null

const taskForm = reactive({
  taskName: '遥感视觉模型评估',
  targetDate: '2026-05-19',
  maskMin: -1,
  maskMax: 1
})

const assetForm = reactive({
  assetName: 'Vision Remote Sensing Mock Evaluator',
  scriptPath: '../scripts/mock_process.py',
  description: 'Mock evaluator for lifecycle scheduling and automated result collection.'
})

function statusType(status) {
  if (status === 'SUCCESS') return 'success'
  if (status === 'FAILED') return 'danger'
  return 'warning'
}

async function refreshTasks() {
  tasks.value = await listTasks()
}

async function refreshAssets() {
  assets.value = await listAssets()
}

async function refreshAll() {
  loading.value = true
  try {
    await Promise.all([refreshTasks(), refreshAssets()])
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

async function submitTask() {
  submitting.value = true
  try {
    await createTask({ ...taskForm })
    ElMessage.success('任务已提交')
    taskDialogVisible.value = false
    await refreshTasks()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    submitting.value = false
  }
}

async function submitAsset() {
  submitting.value = true
  try {
    await createAsset({ ...assetForm })
    ElMessage.success('资产已登记')
    assetDialogVisible.value = false
    await refreshAssets()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    submitting.value = false
  }
}

async function openResult(task) {
  selectedTask.value = task
  cacheBust.value = Date.now()
  try {
    const [csvData, imageData] = await Promise.all([getTaskCsv(task.id), getTaskImages(task.id)])
    csv.headers = csvData.headers
    csv.rows = csvData.rows
    images.value = imageData
    resultVisible.value = true
  } catch (error) {
    ElMessage.error(error.message)
  }
}

onMounted(async () => {
  await refreshAll()
  pollTimer = window.setInterval(async () => {
    if (tasks.value.some((task) => task.status === 'RUNNING')) {
      await refreshTasks()
    }
  }, 3000)
})

onUnmounted(() => {
  if (pollTimer) {
    window.clearInterval(pollTimer)
  }
})
</script>

