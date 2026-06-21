<template>
  <div class="admin-dashboard">
    <el-row :gutter="20">
      <el-col :span="6" v-for="(val, key) in overview" :key="key">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">{{ labels[key] }}</div>
          <div class="stat-value">{{ val }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="mt-20">
      <el-col :span="12">
        <el-card header="活动类型分布">
          <div ref="typeChartRef" style="height: 350px;"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card header="参与人次趋势">
          <div ref="trendChartRef" style="height: 350px;"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="mt-20">
      <el-col :span="24">
        <el-card header="当月各社团预算占用率">
          <div ref="budgetChartRef" style="height: 400px;"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="mt-20">
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>反馈情绪分布</span>
              <el-select v-model="selectedActivityForStats" placeholder="选择活动" clearable @change="loadSentimentStats" size="small">
                <el-option 
                  v-for="act in activities" 
                  :key="act.id" 
                  :label="act.title" 
                  :value="act.id" />
              </el-select>
            </div>
          </template>
          <div v-if="sentimentStats" class="sentiment-stats-inline">
            <el-row :gutter="15">
              <el-col :span="8">
                <div class="mini-stat positive">
                  <div class="mini-stat-label">正面</div>
                  <div class="mini-stat-value">{{ sentimentStats.positiveCount }}</div>
                  <div class="mini-stat-percent">{{ sentimentStats.positivePercentage }}%</div>
                </div>
              </el-col>
              <el-col :span="8">
                <div class="mini-stat neutral">
                  <div class="mini-stat-label">中性</div>
                  <div class="mini-stat-value">{{ sentimentStats.neutralCount }}</div>
                  <div class="mini-stat-percent">{{ sentimentStats.neutralPercentage }}%</div>
                </div>
              </el-col>
              <el-col :span="8">
                <div class="mini-stat negative">
                  <div class="mini-stat-label">负面</div>
                  <div class="mini-stat-value">{{ sentimentStats.negativeCount }}</div>
                  <div class="mini-stat-percent">{{ sentimentStats.negativePercentage }}%</div>
                </div>
              </el-col>
            </el-row>
            <div ref="sentimentChartRef" style="height: 250px; margin-top: 20px;"></div>
          </div>
          <el-empty v-else-if="!sentimentLoading" description="选择活动查看情绪统计" />
          <div v-else style="text-align: center; padding: 40px;">
            <el-icon class="is-loading" style="font-size: 32px;"><Loading /></el-icon>
            <p style="margin-top: 10px; color: #909399;">加载中...</p>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card header="数据导出">
          <div class="export-section">
            <el-form :model="exportForm" label-width="100px">
              <el-form-item label="导出类型">
                <el-radio-group v-model="exportForm.type">
                  <el-radio value="clubs">社团列表</el-radio>
                  <el-radio value="activities">活动列表</el-radio>
                  <el-radio value="logs">操作日志</el-radio>
                  <el-radio value="feedback">活动反馈</el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="按情绪筛选" v-if="exportForm.type === 'feedback'">
                <el-select v-model="exportForm.sentiment" placeholder="全部" clearable>
                  <el-option label="正面" value="POSITIVE" />
                  <el-option label="中性" value="NEUTRAL" />
                  <el-option label="负面" value="NEGATIVE" />
                </el-select>
              </el-form-item>
              <el-form-item label="筛选活动" v-if="exportForm.type === 'feedback'">
                <el-select v-model="exportForm.activityId" placeholder="全部活动" clearable>
                  <el-option 
                    v-for="act in activities" 
                    :key="act.id" 
                    :label="act.title" 
                    :value="act.id" />
                </el-select>
              </el-form-item>
            </el-form>
            <el-button type="primary" @click="handleExport" :loading="exporting">
              <el-icon><Download /></el-icon> 导出 Excel
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive, watch } from 'vue'
import * as echarts from 'echarts'
import request, { downloadFile, getFilenameFromContentDisposition } from '../../utils/request'
import { ElMessage } from 'element-plus'
import { Download, Loading } from '@element-plus/icons-vue'

const overview = reactive({
  totalClubs: 0,
  totalActivities: 0,
  totalRegistrations: 0,
  totalInteractions: 0
})

const labels: Record<string, string> = {
  totalClubs: '全校社团数量',
  totalActivities: '活动总数',
  totalRegistrations: '累计参与人次',
  totalInteractions: '互动总量'
}

const typeChartRef = ref<HTMLElement | null>(null)
const trendChartRef = ref<HTMLElement | null>(null)
const budgetChartRef = ref<HTMLElement | null>(null)
const sentimentChartRef = ref<HTMLElement | null>(null)

const activities = ref<any[]>([])
const selectedActivityForStats = ref<number | null>(null)
const sentimentStats = ref<any>(null)
const sentimentLoading = ref(false)
let sentimentChart: echarts.ECharts | null = null

const exportForm = ref({
  type: 'clubs',
  sentiment: '',
  activityId: null as number | null
})
const exporting = ref(false)

const loadActivities = async () => {
  try {
    const res: any = await request.get('/activities')
    activities.value = res || []
  } catch (err) {
    console.error('Failed to load activities:', err)
  }
}

const loadSentimentStats = async (activityId: number | null) => {
  if (!activityId) {
    sentimentStats.value = null
    return
  }
  sentimentLoading.value = true
  try {
    const res: any = await request.get(`/activities/${activityId}/feedback-stats`)
    sentimentStats.value = res
    renderSentimentChart()
  } catch (err: any) {
    console.error('Failed to load sentiment stats:', err)
    ElMessage.error('加载情绪统计失败')
  } finally {
    sentimentLoading.value = false
  }
}

const renderSentimentChart = () => {
  if (!sentimentChartRef.value || !sentimentStats.value) return
  
  if (sentimentChart) {
    sentimentChart.dispose()
  }
  
  sentimentChart = echarts.init(sentimentChartRef.value)
  
  const data = [
    { value: sentimentStats.value.positiveCount, name: '正面', itemStyle: { color: '#67C23A' } },
    { value: sentimentStats.value.neutralCount, name: '中性', itemStyle: { color: '#909399' } },
    { value: sentimentStats.value.negativeCount, name: '负面', itemStyle: { color: '#F56C6C' } }
  ]
  
  sentimentChart.setOption({
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} ({d}%)'
    },
    legend: {
      bottom: 0,
      data: ['正面', '中性', '负面']
    },
    series: [{
      name: '情绪分布',
      type: 'pie',
      radius: ['40%', '70%'],
      avoidLabelOverlap: false,
      itemStyle: {
        borderRadius: 10,
        borderColor: '#fff',
        borderWidth: 2
      },
      label: {
        show: false,
        position: 'center'
      },
      emphasis: {
        label: {
          show: true,
          fontSize: 20,
          fontWeight: 'bold'
        }
      },
      labelLine: {
        show: false
      },
      data: data
    }]
  })
}

const handleExport = async () => {
  try {
    exporting.value = true
    let url = `/admin/export/${exportForm.value.type}`
    const queryParams = new URLSearchParams()
    
    if (exportForm.value.type === 'feedback') {
      if (exportForm.value.sentiment) {
        queryParams.append('sentiment', exportForm.value.sentiment)
      }
      if (exportForm.value.activityId) {
        queryParams.append('activityId', String(exportForm.value.activityId))
      }
    }
    
    const queryString = queryParams.toString()
    if (queryString) url += `?${queryString}`
    
    const resp: any = await request.get(url, { responseType: 'blob' })
    const defaultNames: Record<string, string> = {
      clubs: '社团列表.xlsx',
      activities: '活动列表.xlsx',
      logs: '操作日志.xlsx',
      feedback: '活动反馈.xlsx'
    }
    const disposition = resp.headers?.['content-disposition'] || null
    const filename = getFilenameFromContentDisposition(disposition, defaultNames[exportForm.value.type] || '导出文件.xlsx')
    const blob = resp.data instanceof Blob ? resp.data : new Blob([resp.data])
    
    if (blob.size < 100) {
      const text = await blob.text()
      if (text.includes('code') && text.includes('message')) {
        try {
          const err = JSON.parse(text)
          ElMessage.error(err.message || '导出失败')
          return
        } catch (_) {}
      }
    }
    
    downloadFile(blob, filename)
    ElMessage.success('导出成功')
  } catch (err: any) {
    console.error('Export failed:', err)
    if (!err?.message?.includes('权限') && !err?.message?.includes('403')) {
      ElMessage.error('导出失败，请稍后重试')
    }
  } finally {
    exporting.value = false
  }
}

onMounted(async () => {
  const res: any = await request.get('/admin/stat/overview')
  Object.assign(overview, res)

  await loadActivities()

  if (typeChartRef.value) {
    const typeRes: any = await request.get('/admin/stat/activity-types')
    const chart = echarts.init(typeChartRef.value)
    chart.setOption({
      tooltip: { trigger: 'item' },
      series: [{
        type: 'pie',
        radius: '70%',
        data: typeRes,
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.5)'
          }
        }
      }]
    })
  }

  if (trendChartRef.value) {
    const trendRes: any = await request.get('/admin/stat/trend')
    const chart = echarts.init(trendChartRef.value)
    chart.setOption({
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: trendRes.dates },
      yAxis: { type: 'value' },
      series: [{
        data: trendRes.values,
        type: 'line',
        smooth: true,
        areaStyle: { opacity: 0.2 }
      }]
    })
  }

  if (budgetChartRef.value) {
    try {
      const budgetRes: any = await request.get('/admin/stat/budget-utilization')
      const chart = echarts.init(budgetChartRef.value)
      const clubNames = budgetRes.map((item: any) => item.clubName)
      const currentBudgets = budgetRes.map((item: any) => item.currentBudget)
      const budgetLimits = budgetRes.map((item: any) => item.budgetLimit)
      const rates = budgetRes.map((item: any) => item.utilizationRate)

      chart.setOption({
        tooltip: {
          trigger: 'axis',
          axisPointer: { type: 'shadow' },
          formatter: (params: any) => {
            let tip = `<strong>${params[0].name}</strong><br/>`
            params.forEach((p: any) => {
              tip += `${p.marker} ${p.seriesName}: ¥${p.value}<br/>`
            })
            const idx = params[0].dataIndex
            tip += `占用率: ${rates[idx]}%`
            return tip
          }
        },
        legend: { data: ['已用预算', '预算上限'] },
        grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
        xAxis: { type: 'category', data: clubNames },
        yAxis: {
          type: 'value',
          axisLabel: { formatter: '¥{value}' }
        },
        series: [
          {
            name: '已用预算',
            type: 'bar',
            data: currentBudgets,
            itemStyle: {
              color: (params: any) => {
                const rate = rates[params.dataIndex]
                if (rate >= 100) return '#F56C6C'
                if (rate >= 80) return '#E6A23C'
                return '#409EFF'
              }
            },
            label: {
              show: true,
              position: 'top',
              formatter: (params: any) => `${rates[params.dataIndex]}%`
            }
          },
          {
            name: '预算上限',
            type: 'bar',
            data: budgetLimits,
            itemStyle: { color: '#DCDFE6' }
          }
        ]
      })
    } catch (err) {
      console.error('Failed to load budget utilization:', err)
    }
  }
})

watch(selectedActivityForStats, (newVal) => {
  loadSentimentStats(newVal)
})
</script>

<style scoped>
.admin-dashboard { padding: 20px; }
.stat-card { text-align: center; }
.stat-label { font-size: 14px; color: #909399; margin-bottom: 10px; }
.stat-value { font-size: 28px; font-weight: bold; color: #409EFF; }
.mt-20 { margin-top: 20px; }
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.sentiment-stats-inline .mini-stat {
  text-align: center;
  padding: 15px;
  border-radius: 8px;
  transition: transform 0.2s;
}
.sentiment-stats-inline .mini-stat:hover {
  transform: translateY(-2px);
}
.sentiment-stats-inline .mini-stat.positive {
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f7fa 100%);
}
.sentiment-stats-inline .mini-stat.neutral {
  background: linear-gradient(135deg, #fafafa 0%, #f5f5f5 100%);
}
.sentiment-stats-inline .mini-stat.negative {
  background: linear-gradient(135deg, #fff5f5 0%, #ffebee 100%);
}
.sentiment-stats-inline .mini-stat-label {
  font-size: 13px;
  color: #909399;
  margin-bottom: 5px;
}
.sentiment-stats-inline .mini-stat-value {
  font-size: 22px;
  font-weight: bold;
}
.sentiment-stats-inline .positive .mini-stat-value {
  color: #67c23a;
}
.sentiment-stats-inline .neutral .mini-stat-value {
  color: #909399;
}
.sentiment-stats-inline .negative .mini-stat-value {
  color: #f56c6c;
}
.sentiment-stats-inline .mini-stat-percent {
  font-size: 12px;
  color: #909399;
  margin-top: 3px;
}
.export-section {
  padding: 10px 0;
}
.export-section :deep(.el-form-item) {
  margin-bottom: 15px;
}
</style>
