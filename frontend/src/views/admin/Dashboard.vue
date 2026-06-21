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
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import * as echarts from 'echarts'
import request from '../../utils/request'

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

onMounted(async () => {
  // Fetch overview
  const res: any = await request.get('/admin/stat/overview')
  Object.assign(overview, res)

  // Type Chart
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

  // Trend Chart
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
})
</script>

<style scoped>
.admin-dashboard { padding: 20px; }
.stat-card { text-align: center; }
.stat-label { font-size: 14px; color: #909399; margin-bottom: 10px; }
.stat-value { font-size: 28px; font-weight: bold; color: #409EFF; }
.mt-20 { margin-top: 20px; }
</style>
