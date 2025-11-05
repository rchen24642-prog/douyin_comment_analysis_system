<template>
  <div class="p-6">
    <h2>情感分析</h2>
    <el-card shadow="hover" class="w-full">

      <div class="flex gap-3 items-center">
        <el-input
            v-model="pid"
            placeholder="输入项目ID，例如 d99755ce-33dc-4359-b41f-1dd660f92025"
            clearable
            style="max-width: 520px;"
        />
        <el-button type="primary" :loading="loading" @click="triggerAnalyze">
          触发情感分析
        </el-button>
      </div>

      <el-divider />

      <!-- 顶层返回展示 -->
      <div v-if="lastResp">
        <el-result
            :icon="lastResp.code === '0' ? 'success' : 'warning'"
            :title="toplineTitle"
            :sub-title="toplineSub"
        >
          <template #extra>
            <el-tag v-if="flask.status"
                    :type="flask.status === 'success' ? 'success' :
                            (flask.status === 'running' ? 'warning' : 'danger')">
              Flask 状态：{{ flask.status }}
            </el-tag>
          </template>
        </el-result>

        <!-- ✅ 新表格显示区域 -->
        <div class="mt-4">
          <el-table
              v-if="tableData.length"
              :data="pagedData"
              border
              style="width: 100%;
              height: calc(100vh - 300px);"
              >
            <el-table-column prop="cid" label="评论编号" width="180"/>
            <el-table-column prop="pid" label="项目编号" width="180"/>
            <el-table-column prop="content" label="评论内容" min-width="250" show-overflow-tooltip/>
            <el-table-column prop="sentimentLabel" label="情感结果" width="120">
              <template #default="scope">
                <el-tag
                    :type="scope.row.sentimentLabel === 1 ? 'success' :
                           (scope.row.sentimentLabel === -1 ? 'danger' : 'info')">
                  {{ scope.row.sentimentLabel === 1 ? '正面' :
                    scope.row.sentimentLabel === -1 ? '负面' : '中性' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="confidenceScore" label="置信度" width="120"/>
            <el-table-column prop="analysisTime" label="分析时间" width="180"/>
          </el-table>
          <p></p>
          <el-pagination
              v-if="tableData.length"
              class="mt-3"
              layout="prev, pager, next, jumper"
              background
              style="text-align: right"
              :page-size="pageSize"
              :current-page="currentPage"
              :total="tableData.length"
              @current-change="handlePageChange"
          />
          <el-empty v-else description="暂无情感分析数据" />
        </div>
      </div>

      <el-empty v-else description="尚未触发分析" class="mt-6" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import requests from '@/utils/requests'

const pid = ref('')
const loading = ref(false)
const lastResp = ref(null)
const flask = ref({ status: '', message: '' })
const rawData = ref('')
const tableData = ref([])

const currentPage = ref(1)
const pageSize = ref(20)

const pagedData = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return tableData.value.slice(start, end)
})

function handlePageChange(page) {
  currentPage.value = page
}


function parseEmbeddedJson(str) {
  if (!str || typeof str !== 'string') return null
  const first = str.indexOf('{')
  const last = str.lastIndexOf('}')
  if (first === -1 || last === -1 || last <= first) return null
  const jsonPart = str.slice(first, last + 1)
  try {
    return JSON.parse(jsonPart)
  } catch {
    return null
  }
}

const toplineTitle = computed(() => {
  if (!lastResp.value) return ''
  return lastResp.value.code === '0' ? '已触发情感分析' : '调用失败'
})

const toplineSub = computed(() => {
  if (!lastResp.value) return ''
  const msg = lastResp.value.msg ?? ''
  return msg || '后端已返回，详情见下方'
})

async function triggerAnalyze() {
  if (!pid.value) {
    lastResp.value = { code: '-1', msg: '请先输入项目ID' }
    flask.value = { status: 'fail', message: '缺少 pid' }
    rawData.value = ''
    return
  }

  loading.value = true
  try {
    const res = await requests.post('/sentiment/analyze', { pid: pid.value })
    lastResp.value = {
      code: String(res?.code ?? '-1'),
      msg: res?.msg ?? '',
      data: res?.data ?? ''
    }

    const embedded = parseEmbeddedJson(String(lastResp.value.data))
    flask.value = {
      status: embedded?.status ?? '',
      message: embedded?.message ?? ''
    }

    // 【修改点】改为调用新的表格数据接口，包含评论内容
    const result = await requests.get(`/sentiment/table/${pid.value}`)
    // 【修改点】为避免优先级问题，加括号
    if (result && (result.code === '0' || result.code === 0 || result.code === '200')) {
      tableData.value = result.data
    } else {
      tableData.value = []
    }

  } catch (e) {
    lastResp.value = { code: '-1', msg: e?.message || '请求异常', data: '' }
    flask.value = { status: 'fail', message: '前端请求异常' }
    tableData.value = []
  } finally {
    loading.value = false
  }
}
</script>


<style scoped>
.p-6 { padding: 24px; }
.mt-4 { margin-top: 16px; }
.mt-6 { margin-top: 24px; }
.w-full { width: 100%; height: 100%; }
</style>
