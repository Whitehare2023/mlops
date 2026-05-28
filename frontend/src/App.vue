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
          <section class="toolbar asset-toolbar">
            <el-input
              v-model="assetKeyword"
              class="asset-search"
              clearable
              placeholder="资产名 / 文件名"
              :prefix-icon="Search"
              @keyup.enter="refreshAssets"
              @clear="refreshAssets"
            />
            <el-button :icon="Search" @click="refreshAssets">查询</el-button>
            <el-button type="primary" :icon="Plus" @click="openCreateAsset">上传资产</el-button>
          </section>

          <el-table :data="assets" class="data-table" stripe>
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="assetName" label="资产名称" min-width="210" />
            <el-table-column prop="originalFilename" label="文件名" min-width="240" />
            <el-table-column label="大小" width="110">
              <template #default="{ row }">{{ formatFileSize(row.fileSize) }}</template>
            </el-table-column>
            <el-table-column prop="contentType" label="文件类型" min-width="160" />
            <el-table-column prop="description" label="说明" min-width="250" />
            <el-table-column prop="updatedAt" label="更新时间" width="190" />
            <el-table-column label="操作" width="260" fixed="right">
              <template #default="{ row }">
                <el-button :icon="Edit" size="small" @click="openEditAsset(row)">编辑</el-button>
                <el-button :icon="Download" size="small" @click="downloadAsset(row)" :disabled="!row.fileSize">
                  下载
                </el-button>
                <el-button :icon="Delete" size="small" type="danger" plain @click="removeAsset(row)">
                  删除
                </el-button>
              </template>
            </el-table-column>
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

  <el-dialog v-model="assetDialogVisible" :title="assetDialogTitle" width="580px">
    <el-form :model="assetForm" label-width="100px">
      <el-form-item label="资产名称">
        <el-input v-model="assetForm.assetName" maxlength="80" />
      </el-form-item>
      <el-form-item label="资产文件" required>
        <el-upload
          class="asset-uploader"
          :auto-upload="false"
          :limit="1"
          :file-list="assetUploadFiles"
          :on-change="handleAssetFileChange"
          :on-remove="handleAssetFileRemove"
          :on-exceed="handleAssetFileExceed"
        >
          <el-button :icon="Upload">选择文件</el-button>
        </el-upload>
        <span v-if="assetMode === 'edit' && assetForm.originalFilename" class="file-current">
          当前：{{ assetForm.originalFilename }}
        </span>
      </el-form-item>
      <el-form-item label="说明">
        <el-input v-model="assetForm.description" type="textarea" :rows="3" maxlength="300" />
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
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Download, Edit, Plus, Refresh, Search, Upload, View } from '@element-plus/icons-vue'
import {
  assetFileUrl,
  createAsset,
  createTask,
  deleteAsset,
  getTaskCsv,
  getTaskImages,
  listAssets,
  listTasks,
  updateAsset
} from './api'

const activeTab = ref('tasks')
const loading = ref(false)
const submitting = ref(false)
const tasks = ref([])
const assets = ref([])
const assetKeyword = ref('')
const csv = reactive({ headers: [], rows: [] })
const images = ref([])
const selectedTask = ref(null)
const resultVisible = ref(false)
const taskDialogVisible = ref(false)
const assetDialogVisible = ref(false)
const assetMode = ref('create')
const selectedAssetFile = ref(null)
const assetUploadFiles = ref([])
const cacheBust = ref(Date.now())
let pollTimer = null

const assetDialogTitle = computed(() => (assetMode.value === 'create' ? '上传模型资产' : '编辑模型资产'))

const taskForm = reactive({
  taskName: '遥感视觉模型评估',
  targetDate: '2026-05-19',
  maskMin: -1,
  maskMax: 1
})

const assetForm = reactive({
  id: null,
  assetName: '',
  originalFilename: '',
  description: ''
})

function statusType(status) {
  if (status === 'SUCCESS') return 'success'
  if (status === 'FAILED') return 'danger'
  return 'warning'
}

function formatFileSize(size) {
  if (!size) return '-'
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / 1024 / 1024).toFixed(2)} MB`
}

async function refreshTasks() {
  tasks.value = await listTasks()
}

async function refreshAssets() {
  assets.value = await listAssets(assetKeyword.value)
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

function openCreateAsset() {
  assetMode.value = 'create'
  assetForm.id = null
  assetForm.assetName = ''
  assetForm.originalFilename = ''
  assetForm.description = ''
  selectedAssetFile.value = null
  assetUploadFiles.value = []
  assetDialogVisible.value = true
}

function openEditAsset(asset) {
  assetMode.value = 'edit'
  assetForm.id = asset.id
  assetForm.assetName = asset.assetName
  assetForm.originalFilename = asset.originalFilename
  assetForm.description = asset.description || ''
  selectedAssetFile.value = null
  assetUploadFiles.value = []
  assetDialogVisible.value = true
}

function handleAssetFileChange(uploadFile, uploadFiles) {
  selectedAssetFile.value = uploadFile.raw
  assetUploadFiles.value = uploadFiles.slice(-1)
}

function handleAssetFileRemove() {
  selectedAssetFile.value = null
  assetUploadFiles.value = []
}

function handleAssetFileExceed(files) {
  selectedAssetFile.value = files[0]
  assetUploadFiles.value = [{ name: files[0].name, raw: files[0] }]
}

async function submitAsset() {
  if (!assetForm.assetName.trim()) {
    ElMessage.error('请输入资产名称')
    return
  }
  if (assetMode.value === 'create' && !selectedAssetFile.value) {
    ElMessage.error('请选择资产文件')
    return
  }

  submitting.value = true
  try {
    const payload = {
      assetName: assetForm.assetName,
      description: assetForm.description,
      file: selectedAssetFile.value
    }
    if (assetMode.value === 'create') {
      await createAsset(payload)
      ElMessage.success('资产已上传')
    } else {
      await updateAsset(assetForm.id, payload)
      ElMessage.success('资产已更新')
    }
    assetDialogVisible.value = false
    await refreshAssets()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    submitting.value = false
  }
}

function downloadAsset(asset) {
  window.open(assetFileUrl(asset.id), '_blank')
}

async function removeAsset(asset) {
  try {
    await ElMessageBox.confirm(`删除资产“${asset.assetName}”？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
    await deleteAsset(asset.id)
    ElMessage.success('资产已删除')
    await refreshAssets()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除已取消')
    }
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
