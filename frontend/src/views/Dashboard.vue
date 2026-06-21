<template>
  <div class="dashboard">
    <el-row :gutter="20">
      <el-col :span="6">
        <div class="stat-card glass-card hover-lift">
          <div class="label">社团总数</div>
          <div class="value">{{ stats.totalClubs }}</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card glass-card hover-lift">
          <div class="label">活跃成员</div>
          <div class="value">{{ stats.totalMembers }}</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card glass-card hover-lift">
          <div class="label">平均满意度</div>
          <div class="value">{{ stats.satisfaction }}<span style="font-size:16px">/5</span></div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card glass-card hover-lift">
          <div class="label">健康活动完成率</div>
          <div class="value">{{ stats.completionRate }}%</div>
        </div>
      </el-col>
    </el-row>

    <div class="chart-container glass-card mt-20" ref="chartRef" style="height: 400px;"></div>
  </div>
</template>

<script setup lang="ts">
import { reactive, onMounted, onUnmounted, ref } from 'vue'
import * as echarts from 'echarts'
import request from '../utils/request'

const stats = reactive({
  totalClubs: 0,
  activeActivities: 0,
  totalMembers: 0,
  completionRate: 0,
  satisfaction: '0.0'
})

const chartRef = ref<HTMLElement | null>(null)
let chart: echarts.ECharts | null = null

onMounted(async () => {
  try {
    const res: any = await request.get('/clubs/dashboard')
    stats.totalClubs = res?.totalClubs ?? 0
    stats.activeActivities = res?.activeActivities ?? 0
    stats.totalMembers = res?.totalMembers ?? 0
    stats.completionRate = res?.completionRate ?? 0
    stats.satisfaction = res?.satisfaction ?? '0.0'
  } catch (err) {
    console.error('Failed to fetch dashboard data:', err)
  }

  if (chartRef.value) {
    chart = echarts.init(chartRef.value)
    chart.setOption({
      title: { text: '活动趋势分析', left: 'center', top: 16 },
      tooltip: { trigger: 'axis' },
      legend: { data: ['新增活动', '参与人数'], bottom: 10 },
      grid: { left: '3%', right: '4%', bottom: '15%', containLabel: true },
      xAxis: {
        type: 'category',
        data: ['1月', '2月', '3月', '4月', '5月', '6月']
      },
      yAxis: { type: 'value' },
      series: [
        {
          name: '新增活动',
          type: 'line',
          smooth: true,
          data: [3, 5, 4, 7, 6, 8],
          itemStyle: { color: '#409eff' },
          areaStyle: { opacity: 0.1 }
        },
        {
          name: '参与人数',
          type: 'bar',
          data: [30, 52, 41, 74, 63, 85],
          itemStyle: { color: '#67c23a' }
        }
      ]
    })
  }
})

onUnmounted(() => {
  chart?.dispose()
})
</script>

<style scoped>
.stat-card {
  padding: 30px;
  text-align: center;
}
.label {
  font-size: 14px;
  color: #666;
  margin-bottom: 10px;
}
.value {
  font-size: 32px;
  font-weight: bold;
  color: #409eff;
}
.mt-20 {
  margin-top: 20px;
}
</style>
