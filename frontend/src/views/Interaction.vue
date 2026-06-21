<template>
  <div class="interaction-container">
    <el-card class="glass-card mb-20">
      <el-tabs v-model="activeTab" class="custom-tabs">
        <el-tab-pane label="公告墙" name="announcements">
          <div class="toolbar mb-20" v-if="['ADMIN', 'UNION_ADMIN', 'CLUB_LEADER'].includes(userStore.role || '')">
            <el-button type="primary" @click="showAddAnnouncement = true">发布公告</el-button>
          </div>
          <el-timeline>
            <el-timeline-item
              v-for="an in announcements"
              :key="an.id"
              :timestamp="formatDate(an.createTime)"
              placement="top"
              :type="an.clubId ? 'primary' : 'danger'"
            >
              <el-card>
                <h4>{{ an.title }}</h4>
                <p>{{ an.content }}</p>
                <div class="meta">
                  <el-tag size="small" :type="an.clubId ? '' : 'danger'">
                    {{ an.clubName }}
                  </el-tag>
                  <span class="author">发布人: {{ an.publisherName }}</span>
                </div>
              </el-card>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-if="!announcements.length" description="暂无公告" />
        </el-tab-pane>

        <el-tab-pane label="讨论区" name="topics">
          <div class="toolbar mb-20" style="display: flex; align-items: center; gap: 15px;">
            <el-button type="primary" @click="openPublishTopic">发帖</el-button>
            <el-radio-group v-model="topicFilter" @change="loadTopics">
              <el-radio-button label="IN_CLUB">社团内部</el-radio-button>
              <el-radio-button label="CROSS_CLUB">跨社公共</el-radio-button>
            </el-radio-group>
            <el-button v-if="['ADMIN', 'UNION_ADMIN'].includes(userStore.role || '')" type="warning" plain @click="showTopicAuditDialog = true">审核跨社话题</el-button>
          </div>
          <div class="topic-list" v-if="topics.length">
            <el-card v-for="t in topics" :key="t.id" class="topic-card mb-20 hover-lift">
              <div class="topic-header">
                <el-avatar :size="40" :src="t.authorAvatar || defaultAvatar" />
                <div class="topic-info">
                  <div class="author-name">{{ t.authorName }}</div>
                  <div class="topic-meta">{{ formatDate(t.createTime) }} · {{ t.clubName }}</div>
                </div>
              </div>
              <h3 class="topic-title">{{ t.title }}</h3>
              <p class="topic-content">{{ t.content }}</p>
              <div class="topic-actions">
                <el-button text :type="t.hasLiked ? 'primary' : ''" @click="interactTopic(t.id, 'LIKE')">
                  👍 {{ t.likesCount }}
                </el-button>
                <el-button text :type="t.hasFavorited ? 'warning' : ''" @click="interactTopic(t.id, 'FAVORITE')">
                  ⭐ {{ t.favoritesCount }}
                </el-button>
              </div>
            </el-card>
          </div>
          <el-empty v-else description="暂无话题" />
        </el-tab-pane>

        <el-tab-pane label="招新通道" name="recruitments">
          <div class="toolbar mb-20" v-if="userStore.role === 'CLUB_LEADER'">
            <el-button type="success" @click="showAddRecruitment = true">发起招新</el-button>
          </div>
          <el-row :gutter="20">
            <el-col :span="8" v-for="r in recruitments" :key="r.id">
              <el-card class="box-card mb-20 border-left-indicator" :class="{ 'inactive': r.status === 'CLOSED' }">
                <template #header>
                  <div class="card-header">
                    <span>{{ r.clubName }}</span>
                    <el-tag :type="r.status === 'OPEN' ? 'success' : 'info'">{{ r.status === 'OPEN' ? '招新中' : '已结课' }}</el-tag>
                  </div>
                </template>
                <h4>{{ r.title }}</h4>
                <p class="desc-line">{{ r.description }}</p>
                <div class="recruitment-actions mt-20" v-if="r.status === 'OPEN'">
                  <el-button v-if="userStore.role === 'CLUB_LEADER'" type="primary" size="small" @click="viewApplications(r.id)">查看简历</el-button>
                  <el-button v-else-if="!r.hasApplied" type="primary" size="small" @click="openApply(r.id)">我要报名</el-button>
                  <el-button v-else type="info" size="small" disabled>已投递</el-button>
                </div>
              </el-card>
            </el-col>
          </el-row>
        </el-tab-pane>

        <el-tab-pane label="问答广场" name="qa">
          <div class="toolbar mb-20" style="display:flex; gap:10px;">
            <el-input v-model="qaSearch" placeholder="搜索问题..." style="width:300px" @keyup.enter="loadQuestions">
              <template #append><el-button @click="loadQuestions">🔍</el-button></template>
            </el-input>
            <el-button type="primary" @click="showAskDialog = true">我要提问</el-button>
          </div>
          <div v-for="q in questions" :key="q.id" class="qa-item glass-card mb-20">
            <h3 class="q-title">Q: {{ q.title }}</h3>
            <p class="q-content">{{ q.content }}</p>
            <div class="q-meta">
              <span>提问者: {{ q.authorName }}</span>
              <span v-if="q.targetClubName">指定社团: {{ q.targetClubName }}</span>
              <span v-if="q.targetRole">指定角色: {{ q.targetRole }}</span>
            </div>
            <div class="q-actions mt-10">
              <el-button type="primary" plain size="small" @click="viewAnswers(q)">查看回答 / 回答</el-button>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- Various Dialogs -->
    
    <!-- Publish Announcement -->
    <el-dialog v-model="showAddAnnouncement" title="发布公告" width="500px">
      <el-form :model="annForm">
        <el-form-item label="标题" required><el-input v-model="annForm.title" /></el-form-item>
        <el-form-item label="内容" required><el-input v-model="annForm.content" type="textarea" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddAnnouncement = false">取消</el-button>
        <el-button type="primary" @click="submitAnnouncement">发布</el-button>
      </template>
    </el-dialog>

    <!-- Publish Topic -->
    <el-dialog v-model="showPublishTopic" title="发帖" width="500px">
      <el-form :model="topicForm" label-width="80px">
        <el-form-item label="类型" required>
          <el-radio-group v-model="topicForm.type">
            <el-radio label="IN_CLUB">社团内部</el-radio>
            <el-radio label="CROSS_CLUB">跨社公共(需审核)</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="标题" required><el-input v-model="topicForm.title" /></el-form-item>
        <el-form-item label="正文" required><el-input v-model="topicForm.content" type="textarea" :rows="4" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showPublishTopic = false">取消</el-button>
        <el-button type="primary" @click="submitTopic">发帖</el-button>
      </template>
    </el-dialog>

    <!-- Audit Topics Dialog (Union Admin) -->
    <el-dialog v-model="showTopicAuditDialog" title="待审批的跨社话题" width="800px">
      <el-table :data="pendingTopics" class="glass-card">
        <el-table-column prop="title" label="标题" />
        <el-table-column prop="authorName" label="发帖人" width="120" />
        <el-table-column label="操作" width="160">
          <template #default="{row}">
            <el-button size="small" type="success" @click="auditTopic(row.id, 'APPROVED')">通过</el-button>
            <el-button size="small" type="danger" @click="auditTopic(row.id, 'REJECTED')">驳回</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- Create Recruitment -->
    <el-dialog v-model="showAddRecruitment" title="发起招新" width="500px">
      <el-form :model="recruitForm" label-width="80px">
        <el-form-item label="招新口号" required><el-input v-model="recruitForm.title" /></el-form-item>
        <el-form-item label="简述/要求" required><el-input v-model="recruitForm.description" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddRecruitment = false">取消</el-button>
        <el-button type="primary" @click="submitRecruitment">发起</el-button>
      </template>
    </el-dialog>

    <!-- Apply Recruitment -->
    <el-dialog v-model="showApplyDialog" title="投递简历" width="500px">
      <el-form>
        <el-form-item label="个人陈述/简历">
          <el-input v-model="applyResumeText" type="textarea" :rows="5" placeholder="请简述您的优势与原因..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showApplyDialog = false">取消</el-button>
        <el-button type="primary" @click="submitApplication">投递</el-button>
      </template>
    </el-dialog>

    <!-- View Applications -->
    <el-dialog v-model="showApplicationsDialog" title="简历处理" width="800px">
      <el-table :data="currentApplications" class="glass-card">
        <el-table-column prop="realName" label="姓名" width="100"/>
        <el-table-column prop="studentId" label="学号" width="120"/>
        <el-table-column prop="resumeText" label="陈述内容"/>
        <el-table-column prop="status" label="状态" width="100">
           <template #default="{row}">
              <el-tag :type="row.status === 'APPROVED' ? 'success' : (row.status === 'REJECTED' ? 'danger' : 'warning')">{{ row.status }}</el-tag>
           </template>
        </el-table-column>
        <el-table-column label="操作" width="150">
          <template #default="{row}">
            <el-button v-if="row.status === 'PENDING'" size="small" type="success" @click="auditApp(row.id, 'APPROVED')">通过</el-button>
            <el-button v-if="row.status === 'PENDING'" size="small" type="danger" @click="auditApp(row.id, 'REJECTED')">淘汰</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- Q&A Answer View Dialog -->
    <el-dialog v-model="showAnswersDialog" title="问题详情与回答" width="700px">
      <div v-if="currentQuestion" class="mb-20">
        <h3>{{ currentQuestion.title }}</h3>
        <p>{{ currentQuestion.content }}</p>
      </div>
      <el-divider />
      <div v-for="ans in currentAnswers" :key="ans.id" class="answer-item mb-20" :class="{ 'best-answer': ans.isBest }">
        <div class="ans-meta">
          <strong>{{ ans.authorName }}</strong> ({{ ans.authorRole }})
          <el-tag v-if="ans.isBest" type="success" size="small" style="margin-left:10px">最佳回答</el-tag>
        </div>
        <p>{{ ans.content }}</p>
        <div v-if="currentQuestion && currentQuestion.authorId === userStore.userInfo?.id && !ans.isBest" style="text-align:right">
          <el-button size="small" type="success" plain @click="markBest(ans.id)">采纳为最佳</el-button>
        </div>
      </div>
      <el-empty v-if="!currentAnswers.length" description="暂无回答" />
      
      <div class="reply-box mt-20">
        <el-input v-model="myAnswer" type="textarea" placeholder="写下你的回答..." />
        <el-button type="primary" class="mt-10" @click="submitAnswer">提交回答</el-button>
      </div>
    </el-dialog>
    
    <!-- Ask Question Dialog -->
    <el-dialog v-model="showAskDialog" title="我要提问" width="500px">
      <el-form :model="askForm" label-width="80px">
        <el-form-item label="标题" required><el-input v-model="askForm.title" /></el-form-item>
        <el-form-item label="内容" required><el-input v-model="askForm.content" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAskDialog = false">取消</el-button>
        <el-button type="primary" @click="submitQuestion">提问</el-button>
      </template>
    </el-dialog>

  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { useUserStore } from '../store/user'
import request from '../utils/request'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()
const activeTab = ref('announcements')
const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea0722952d4a0e3e8b0e8e8e8e8e.png'

// ---------- Announcements
const announcements = ref<any[]>([])
const showAddAnnouncement = ref(false)
const annForm = reactive({ title: '', content: '' })

const loadAnnouncements = async () => {
  try {
    announcements.value = (await request.get('/announcements')) as any
  } catch (err) {
    console.error('Failed to fetch announcements:', err)
  }
}
const submitAnnouncement = async () => {
  try {
    await request.post('/announcements', annForm)
    ElMessage.success('发布成功')
    showAddAnnouncement.value = false
    loadAnnouncements()
  } catch (err) {
    console.error('Announcement submission failed:', err)
  }
}

// ---------- Topics
const topics = ref<any[]>([])
const pendingTopics = ref<any[]>([])
const topicFilter = ref('IN_CLUB')
const showPublishTopic = ref(false)
const showTopicAuditDialog = ref(false)
const topicForm = reactive({ title: '', content: '', type: 'IN_CLUB' })

const loadTopics = async () => {
  try {
    topics.value = (await request.get(`/topics?type=${topicFilter.value}`)) as any
  } catch (err) {
    console.error('Failed to fetch topics:', err)
  }
}
const openPublishTopic = () => {
  topicForm.title = ''
  topicForm.content = ''
  topicForm.type = 'IN_CLUB'
  showPublishTopic.value = true
}
const submitTopic = async () => {
  try {
    await request.post('/topics', topicForm)
    if (topicForm.type === 'CROSS_CLUB') {
      ElMessage.success('发帖成功，请等待跨社话题审核')
    } else {
      ElMessage.success('发帖成功')
    }
    showPublishTopic.value = false
    topicFilter.value = topicForm.type
    loadTopics()
  } catch (err) {
    console.error('Topic submission failed:', err)
  }
}
const interactTopic = async (id: number, type: string) => {
  try {
    await request.post(`/topics/${id}/interact`, { type })
    loadTopics()
  } catch (err) {
    console.error('Topic interaction failed:', err)
  }
}
const loadPendingTopics = async () => {
  try {
    pendingTopics.value = (await request.get('/topics/pending')) as any
  } catch (err) {
    console.error('Failed to fetch pending topics:', err)
  }
}
const auditTopic = async (id: number, status: string) => {
  try {
    await request.post(`/topics/${id}/audit`, { status })
    ElMessage.success('处理完成')
    loadPendingTopics()
  } catch (err) {
    console.error('Topic audit failed:', err)
  }
}

// ---------- Recruitments
const recruitments = ref<any[]>([])
const showAddRecruitment = ref(false)
const recruitForm = reactive({ title: '', description: '' })
const showApplyDialog = ref(false)
const currentApplyRecruitId = ref(0)
const applyResumeText = ref('')
const showApplicationsDialog = ref(false)
const currentApplications = ref<any[]>([])

const loadRecruitments = async () => {
  try {
    recruitments.value = (await request.get('/recruitments')) as any
  } catch (err) {
    console.error('Failed to fetch recruitments:', err)
  }
}
const submitRecruitment = async () => {
  try {
    await request.post('/recruitments', recruitForm)
    ElMessage.success('招新发布成功')
    showAddRecruitment.value = false
    loadRecruitments()
  } catch (err) {
    console.error('Recruitment submission failed:', err)
  }
}
const openApply = (id: number) => {
  currentApplyRecruitId.value = id
  applyResumeText.value = ''
  showApplyDialog.value = true
}
const submitApplication = async () => {
  try {
    await request.post(`/recruitments/${currentApplyRecruitId.value}/apply`, { resumeText: applyResumeText.value })
    ElMessage.success('投递成功')
    showApplyDialog.value = false
    loadRecruitments()
  } catch (err) {
    console.error('Recruitment application failed:', err)
  }
}
const viewApplications = async (id: number) => {
  try {
    currentApplications.value = (await request.get(`/recruitments/${id}/applications`)) as any
    showApplicationsDialog.value = true
  } catch (err) {
    console.error('Failed to fetch applications:', err)
  }
}
const auditApp = async (id: number, status: string) => {
  try {
    await request.post(`/recruitments/applications/${id}/audit`, { status })
    ElMessage.success('处理完成')
    showApplicationsDialog.value = false
  } catch (err) {
    console.error('Application audit failed:', err)
  }
}

// ---------- Q&A
const questions = ref<any[]>([])
const qaSearch = ref('')
const showAskDialog = ref(false)
const askForm = reactive({ title: '', content: '' })
const showAnswersDialog = ref(false)
const currentQuestion = ref<any>(null)
const currentAnswers = ref<any[]>([])
const myAnswer = ref('')

const loadQuestions = async () => {
  try { 
    const query = qaSearch.value ? `?keyword=${qaSearch.value}` : ''
    questions.value = (await request.get(`/qa/questions${query}`)) as any
  } catch (err) {
    console.error('Failed to fetch questions:', err)
  }
}
const submitQuestion = async () => {
  try {
    await request.post('/qa/questions', askForm)
    ElMessage.success('提问成功')
    showAskDialog.value = false
    loadQuestions()
  } catch (err) {
    console.error('Question submission failed:', err)
  }
}
const viewAnswers = async (q: any) => {
  currentQuestion.value = q
  myAnswer.value = ''
  await loadAnswers(q.id)
  showAnswersDialog.value = true
}
const loadAnswers = async (qid: number) => {
  try {
    currentAnswers.value = (await request.get(`/qa/questions/${qid}/answers`)) as any
  } catch (err) {
    console.error('Failed to fetch answers:', err)
  }
}
const submitAnswer = async () => {
  try {
    await request.post('/qa/answers', { questionId: currentQuestion.value.id, content: myAnswer.value })
    ElMessage.success('回答成功')
    myAnswer.value = ''
    loadAnswers(currentQuestion.value.id)
  } catch (err) {
    console.error('Answer submission failed:', err)
  }
}
const markBest = async (ansId: number) => {
  try {
    await request.post(`/qa/answers/${ansId}/best`)
    ElMessage.success('采纳成功')
    loadAnswers(currentQuestion.value.id)
  } catch (err) {
    console.error('Mark best answer failed:', err)
  }
}

// Utility
const formatDate = (dt: string) => {
  if (!dt) return '-'
  return new Date(dt).toLocaleString('zh-CN', { hour12: false })
}

watch(activeTab, (val: string) => {
  if (val === 'announcements') loadAnnouncements()
  else if (val === 'topics') { loadTopics(); if(['ADMIN', 'UNION_ADMIN'].includes(userStore.role || '')) loadPendingTopics() }
  else if (val === 'recruitments') loadRecruitments()
  else if (val === 'qa') loadQuestions()
})

onMounted(() => {
  loadAnnouncements()
})
</script>

<style scoped>
.interaction-container {
  padding: 10px;
}
.mb-20 { margin-bottom: 20px; }
.mt-10 { margin-top: 10px; }
.mt-20 { margin-top: 20px; }
.meta { margin-top: 10px; font-size: 13px; color: #999; display: flex; justify-content: space-between; }
.card-header { display: flex; justify-content: space-between; align-items: center; }

.topic-card { padding: 15px; border-radius: 8px; }
.topic-header { display: flex; align-items: center; gap: 15px; margin-bottom: 12px; }
.topic-info .author-name { font-weight: bold; font-size: 15px; }
.topic-info .topic-meta { font-size: 12px; color: #888; }
.topic-title { font-size: 18px; margin: 0 0 10px; }
.topic-content { font-size: 14px; color: #444; line-height: 1.6; }
.topic-actions { margin-top: 15px; display: flex; gap: 20px; }

.border-left-indicator { border-left: 4px solid #409eff; }
.inactive { border-left: 4px solid #909399; opacity: 0.8;}
.desc-line { height: 40px; overflow: hidden; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; font-size: 14px; color: #666; }

.qa-item { padding: 20px; }
.q-title { font-size: 16px; margin: 0 0 5px; color: #303133; }
.q-content { font-size: 14px; color: #606266; margin-bottom: 10px; }
.q-meta { font-size: 12px; color: #909399; display: flex; gap: 15px; }

.answer-item { padding: 15px; background: #f5f7fa; border-radius: 6px; }
.best-answer { border: 1px solid #67c23a; background: #f0f9eb; }
.ans-meta { margin-bottom: 8px; font-size: 13px; color: #303133; }
</style>
