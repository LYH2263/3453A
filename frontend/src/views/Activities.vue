<template>
  <div class="activities-page">
    <div class="toolbar glass-card mb-20" v-if="userStore.role && ['ADMIN', 'UNION_ADMIN', 'CLUB_LEADER'].includes(userStore.role)">
      <el-button type="success" :icon="Plus" @click="showAddDialog = true">发起活动</el-button>
      <div class="view-switch-wrapper" v-show="activeTab === 'all'">
        <el-radio-group v-model="viewMode" size="default" @change="handleViewModeChange">
          <el-radio-button label="list">
            <el-icon><List /></el-icon>
            <span>列表视图</span>
          </el-radio-button>
          <el-radio-button label="calendar">
            <el-icon><Calendar /></el-icon>
            <span>日历视图</span>
          </el-radio-button>
        </el-radio-group>
      </div>
    </div>
    <div class="toolbar glass-card mb-20 view-switch-toolbar" v-if="!userStore.role || !['ADMIN', 'UNION_ADMIN', 'CLUB_LEADER'].includes(userStore.role)">
      <div class="view-switch-wrapper" v-show="activeTab === 'all'">
        <el-radio-group v-model="viewMode" size="default" @change="handleViewModeChange">
          <el-radio-button label="list">
            <el-icon><List /></el-icon>
            <span>列表视图</span>
          </el-radio-button>
          <el-radio-button label="calendar">
            <el-icon><Calendar /></el-icon>
            <span>日历视图</span>
          </el-radio-button>
        </el-radio-group>
      </div>
    </div>

    <el-tabs v-model="activeTab" class="mb-20">
      <el-tab-pane label="全部活动" name="all" />
      <el-tab-pane label="待我确认" name="pending" v-if="userStore.role === 'CLUB_LEADER'">
        <span style="position: relative">
          待我确认
          <el-badge v-if="pendingCoHosts.length > 0" :value="pendingCoHosts.length" :max="99" class="tab-badge" />
        </span>
      </el-tab-pane>
    </el-tabs>

    <div v-show="activeTab === 'all'">
      <div v-show="viewMode === 'list'">
        <el-row :gutter="20">
          <el-col :span="8" v-for="act in activities" :key="act.id" class="mb-20">
          <el-card class="act-card hover-lift glass-card" :body-style="{ padding: '0px' }">
            <div class="card-cover" v-if="act.poster">
              <el-image :src="act.poster" fit="cover" style="width: 100%; height: 120px" />
            </div>
            <div class="card-header">
              <h3>{{ act.title }}</h3>
              <el-tag :type="statusType(act.status)">{{ statusText(act.status) }}</el-tag>
            </div>
            <div class="card-body">
              <p class="desc">{{ act.description }}</p>
              <div class="info">
                <span><el-icon><Location /></el-icon> {{ act.location }}</span>
                <span><el-icon><Calendar /></el-icon> {{ act.startTime?.split('T')[0] }}</span>
              </div>
              <div class="club-info" v-if="act._detail?.hostClub">
                <span>主办: {{ act._detail.hostClub.name }}</span>
              </div>
              <div class="co-host-info" v-if="act._detail?.coHosts && act._detail.coHosts.length > 0">
                <span>
                  合作: 
                  <span v-for="(co, idx) in act._detail.coHosts" :key="co.id">
                    {{ co.club?.name }}
                    <el-tag :type="coHostStatusType(co.status)" size="small" style="margin-left: 4px">
                      {{ coHostStatusText(co.status) }}
                    </el-tag>
                    <span v-if="idx < act._detail.coHosts.length - 1">, </span>
                  </span>
                </span>
              </div>
              <div class="budget-info" v-if="act.budget > 0">
                <span>预算: ¥{{ act.budget }}</span>
              </div>
              <div class="enrollment-info">
                <span class="enrollment-count">
                  已报名: {{ act._detail?.registeredCount || 0 }} / {{ act.maxCount }}
                  <span v-if="act._detail?.waitlistCount && act._detail.waitlistCount > 0" class="waitlist-count">
                    (候补: {{ act._detail?.waitlistCount }})
                  </span>
                </span>
                <el-tag v-if="act._detail?.isFull" type="danger" size="small">已满员</el-tag>
              </div>
              <div class="my-status" v-if="act._detail?.myStatus && act._detail.myStatus !== 'CANCELLED'">
                <el-tag :type="myStatusType(act._detail.myStatus)" size="small">
                  {{ myStatusText(act._detail) }}
                </el-tag>
              </div>
              <div class="actions">
                <template v-if="act.status === 'APPROVED'">
                  <template v-if="!act._detail?.myStatus || act._detail.myStatus === 'CANCELLED'">
                    <el-button 
                      :type="act._detail?.isFull ? 'warning' : 'primary'" 
                      size="small" 
                      @click="handleRegister(act)">
                      {{ act._detail?.isFull ? '加入候补' : '立即报名' }}
                    </el-button>
                  </template>
                  <template v-else-if="act._detail.myStatus === 'WAITLIST'">
                    <el-button type="info" size="small" @click="handleLeaveWaitlist(act)">
                      取消候补
                    </el-button>
                  </template>
                  <template v-else-if="act._detail.myStatus === 'REGISTERED'">
                    <el-button type="danger" size="small" @click="handleCancel(act)">
                      取消报名
                    </el-button>
                  </template>
                  <el-button 
                    v-if="canExpand(act)" 
                    type="success" 
                    size="small" 
                    @click="handleExpand(act)">
                    扩容
                  </el-button>
                </template>
                <el-button v-if="canEdit(act)" type="primary" size="small" @click="handleEdit(act)">编辑</el-button>
                <el-button v-if="canDelete(act)" type="danger" size="small" @click="handleDelete(act)">删除</el-button>
                <el-button v-if="canAudit(act)" type="warning" size="small" @click="handleAudit(act)">审核</el-button>
                <el-button v-if="canConfirmCoHost(act)" type="success" size="small" @click="handleCoHostConfirm(act)">
                  确认合作
                </el-button>
                <el-button v-if="act.status === 'APPROVED' && userStore.role === 'CLUB_LEADER'" type="success" size="small" @click="handleFinish(act.id)">结束活动</el-button>
                <el-button 
                  v-if="canFeedback(act)" 
                  type="primary" 
                  size="small" 
                  @click="handleFeedback(act)">
                  反馈
                </el-button>
                <el-button 
                  v-if="canViewStats(act)" 
                  type="warning" 
                  size="small" 
                  @click="handleViewStats(act)">
                  反馈统计
                </el-button>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
      </div>

      <div v-show="viewMode === 'calendar'" class="calendar-view">
        <el-calendar v-model="calendarCurrentDate">
          <template #date-cell="{ data }">
            <div
              class="calendar-cell"
              :class="{ 'is-selected': selectedDate && formatDateKey(selectedDate) === data.day, 'is-today': isToday(data.day) }"
              @click="handleDateClick(data.day)">
              <div class="cell-date">{{ data.day.split('-').slice(2).join('') }}</div>
              <div class="cell-activities">
                <div
                  v-for="act in getActivitiesForDate(data.day)"
                  :key="act.id"
                  class="cell-activity-item"
                  :class="getCalendarActivityClass(act)"
                  @click.stop="handleCalendarActivityClick(act)">
                  <span class="dot"></span>
                  <span class="act-title">{{ truncateText(act.title, 6) }}</span>
                </div>
                <div v-if="getActivitiesForDate(data.day).length > 3" class="cell-more">
                  +{{ getActivitiesForDate(data.day).length - 3 }}
                </div>
              </div>
            </div>
          </template>
        </el-calendar>
      </div>
    </div>

    <el-dialog v-model="showDayActivitiesDialog" :title="selectedDateTitle" width="600px" class="day-activities-dialog">
      <div v-if="selectedDateActivities.length === 0" class="empty-day">
        <el-empty description="该日期暂无活动" :image-size="80" />
      </div>
      <div v-else class="day-activities-list">
        <div
          v-for="act in selectedDateActivities"
          :key="act.id"
          class="day-activity-card glass-card">
          <div class="day-activity-header">
            <h4 class="day-activity-title">{{ act.title }}</h4>
            <el-tag :type="statusType(act.status)" size="small">{{ statusText(act.status) }}</el-tag>
          </div>
          <div class="day-activity-meta">
            <span class="meta-item">
              <el-icon><Clock /></el-icon>
              {{ formatActivityTime(act) }}
            </span>
            <span class="meta-item">
              <el-icon><Location /></el-icon>
              {{ act.location }}
            </span>
          </div>
          <div class="day-activity-actions">
            <el-button v-if="canAuditFromCalendar(act)" type="warning" size="small" @click="handleAuditFromCalendar(act)">
              审核
            </el-button>
            <template v-if="act.status === 'APPROVED'">
              <template v-if="!act._detail?.myStatus || act._detail?.myStatus === 'CANCELLED'">
                <el-button
                  :type="act._detail?.isFull ? 'warning' : 'primary'"
                  size="small"
                  @click="handleRegisterFromCalendar(act)">
                  {{ act._detail?.isFull ? '加入候补' : '立即报名' }}
                </el-button>
              </template>
              <template v-else-if="act._detail?.myStatus === 'WAITLIST'">
                <el-button type="info" size="small" @click="handleLeaveWaitlist(findActivityById(act.id))">
                  取消候补
                </el-button>
              </template>
              <template v-else-if="act._detail?.myStatus === 'REGISTERED'">
                <el-button type="danger" size="small" @click="handleCancel(findActivityById(act.id))">
                  取消报名
                </el-button>
              </template>
              <el-button v-if="canExpand(findActivityById(act.id))" type="success" size="small" @click="handleExpand(findActivityById(act.id))">
                扩容
              </el-button>
            </template>
            <el-button v-if="canEdit(findActivityById(act.id))" type="primary" size="small" @click="handleEdit(findActivityById(act.id))">编辑</el-button>
            <el-button v-if="canDelete(findActivityById(act.id))" type="danger" size="small" @click="handleDelete(findActivityById(act.id))">删除</el-button>
            <el-button
              v-if="canFeedback(findActivityById(act.id))"
              type="primary"
              size="small"
              @click="handleFeedback(findActivityById(act.id))">
              反馈
            </el-button>
            <el-button
              v-if="canViewStats(findActivityById(act.id))"
              type="warning"
              size="small"
              @click="handleViewStats(findActivityById(act.id))">
              反馈统计
            </el-button>
            <el-button
              v-if="act.status === 'APPROVED' && userStore.role === 'CLUB_LEADER' && findActivityById(act.id)?.clubId === userStore.userInfo?.clubId"
              type="success"
              size="small"
              @click="handleFinish(act.id)">
              结束活动
            </el-button>
          </div>
        </div>
      </div>
    </el-dialog>

    <div v-show="activeTab === 'pending'">
      <el-empty v-if="pendingCoHosts.length === 0" description="暂无待确认的合作活动" />
      <el-row :gutter="20">
        <el-col :span="8" v-for="item in pendingCoHosts" :key="item.coHostId" class="mb-20">
          <el-card class="act-card hover-lift glass-card" :body-style="{ padding: '0px' }">
            <div class="card-header">
              <h3>{{ item.activityTitle }}</h3>
              <el-tag type="warning">待确认</el-tag>
            </div>
            <div class="card-body">
              <p class="desc">{{ item.activityDescription }}</p>
              <div class="info">
                <span><el-icon><Location /></el-icon> {{ item.location }}</span>
                <span><el-icon><Calendar /></el-icon> {{ item.startTime?.split('T')[0] }}</span>
              </div>
              <div class="club-info" v-if="item.hostClub">
                <span>主办方: {{ item.hostClub.name }}</span>
              </div>
              <div class="budget-info" v-if="item.budget > 0">
                <span>预算: ¥{{ item.budget }}</span>
              </div>
              <div class="actions">
                <el-button type="success" size="small" @click="handleConfirmFromList(item)">确认合作</el-button>
                <el-button type="danger" size="small" @click="handleRejectFromList(item)">拒绝</el-button>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <el-dialog v-model="showAddDialog" title="发起活动" width="600px">
      <el-form :model="addForm" ref="addFormRef" label-width="80px">
        <el-form-item label="活动名称" required>
          <el-input v-model="addForm.title" />
        </el-form-item>
        <el-form-item label="活动描述" required>
          <el-input v-model="addForm.description" type="textarea" />
        </el-form-item>
        <el-form-item label="活动流程" required>
          <el-input v-model="addForm.process" type="textarea" placeholder="填写活动环节及时间节点" />
        </el-form-item>
        <el-form-item label="活动地点" required>
          <el-input v-model="addForm.location" />
        </el-form-item>
        <el-form-item label="开始时间" required>
          <el-date-picker v-model="addForm.startTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" />
        </el-form-item>
        <el-form-item label="结束时间" required>
          <el-date-picker v-model="addForm.endTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" />
        </el-form-item>
        <el-form-item label="人数上限">
          <el-input-number v-model="addForm.maxCount" :min="1" />
        </el-form-item>
        <el-form-item label="预算">
          <el-input-number v-model="addForm.budget" :min="0" />
        </el-form-item>
        <el-form-item label="合作社团" v-if="userStore.role === 'CLUB_LEADER'">
          <el-select
            v-model="addForm.coHostClubIds"
            multiple
            filterable
            placeholder="请选择合作社团（可选多个）"
            style="width: 100%"
            :disabled="clubOptions.length === 0">
            <el-option
              v-for="club in clubOptions"
              :key="club.id"
              :label="club.name"
              :value="club.id" />
          </el-select>
          <div class="form-tip">选择合作社团后，活动需经所有合作社团负责人确认后才进入审核流程</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" @click="submitAdd">确认发起</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showBudgetWarningDialog" title="预算超限警告" width="480px">
      <el-alert type="warning" :closable="false" style="margin-bottom: 15px">
        <p>该社团当月预算合计已超出月度预算上限！</p>
        <p>当月已用：¥{{ budgetWarningInfo.currentTotal }}</p>
        <p>提交后合计：¥{{ budgetWarningInfo.projectedTotal }}</p>
        <p>月度上限：¥{{ budgetWarningInfo.limit }}</p>
      </el-alert>
      <el-checkbox v-model="budgetRiskAcknowledged">
        我已知晓超支风险，确认继续发起
      </el-checkbox>
      <template #footer>
        <el-button @click="showBudgetWarningDialog = false">取消</el-button>
        <el-button type="primary" :disabled="!budgetRiskAcknowledged" @click="forceSubmitAdd">确认发起</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showEditDialog" title="编辑活动" width="500px">
      <el-form :model="editForm" ref="editFormRef" label-width="80px">
        <el-form-item label="活动名称" required>
          <el-input v-model="editForm.title" />
        </el-form-item>
        <el-form-item label="活动描述" required>
          <el-input v-model="editForm.description" type="textarea" />
        </el-form-item>
        <el-form-item label="活动流程" required>
          <el-input v-model="editForm.process" type="textarea" />
        </el-form-item>
        <el-form-item label="活动地点" required>
          <el-input v-model="editForm.location" />
        </el-form-item>
        <el-form-item label="开始时间" required>
          <el-date-picker v-model="editForm.startTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" />
        </el-form-item>
        <el-form-item label="结束时间" required>
          <el-date-picker v-model="editForm.endTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" />
        </el-form-item>
        <el-form-item label="人数上限">
          <el-input-number v-model="editForm.maxCount" :min="1" />
        </el-form-item>
        <el-form-item label="预算">
          <el-input-number v-model="editForm.budget" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" @click="submitEdit">保存修改</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showBudgetWarningEditDialog" title="预算超限警告" width="480px">
      <el-alert type="warning" :closable="false" style="margin-bottom: 15px">
        <p>修改后该社团当月预算合计将超出月度预算上限！</p>
        <p>当月已用：¥{{ budgetWarningInfo.currentTotal }}</p>
        <p>修改后合计：¥{{ budgetWarningInfo.projectedTotal }}</p>
        <p>月度上限：¥{{ budgetWarningInfo.limit }}</p>
      </el-alert>
      <el-checkbox v-model="budgetRiskAcknowledged">
        我已知晓超支风险，确认继续修改
      </el-checkbox>
      <template #footer>
        <el-button @click="showBudgetWarningEditDialog = false">取消</el-button>
        <el-button type="primary" :disabled="!budgetRiskAcknowledged" @click="forceSubmitEdit">确认修改</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showAuditDialog" title="活动审核" width="400px">
      <el-form :model="auditForm" label-width="80px">
        <el-form-item label="审核结果">
          <el-radio-group v-model="auditForm.status">
            <el-radio label="APPROVED">通过</el-radio>
            <el-radio label="REJECTED">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注说明" v-if="auditForm.status === 'REJECTED'">
          <el-input v-model="auditForm.reason" type="textarea" placeholder="请输入驳回原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAuditDialog = false">取消</el-button>
        <el-button type="primary" @click="submitAudit">确认</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showExpandDialog" title="活动扩容" width="400px">
      <el-form :model="expandForm" label-width="100px">
        <el-form-item label="活动名称">
          <span>{{ expandForm.activityTitle }}</span>
        </el-form-item>
        <el-form-item label="当前上限">
          <span>{{ expandForm.currentMaxCount }}</span>
        </el-form-item>
        <el-form-item label="当前报名">
          <span>{{ expandForm.registeredCount }} 人</span>
        </el-form-item>
        <el-form-item label="候补人数">
          <span>{{ expandForm.waitlistCount }} 人</span>
        </el-form-item>
        <el-form-item label="新的上限" required>
          <el-input-number 
            v-model="expandForm.newMaxCount" 
            :min="expandForm.currentMaxCount + 1"
            :max="9999" />
        </el-form-item>
        <div v-if="expandForm.waitlistCount > 0" class="expand-hint">
          <el-icon><InfoFilled /></el-icon>
          扩容后将自动递补最多 {{ Math.min(expandForm.waitlistCount, expandForm.newMaxCount - expandForm.currentMaxCount) }} 名候补给用户
        </div>
      </el-form>
      <template #footer>
        <el-button @click="showExpandDialog = false">取消</el-button>
        <el-button type="primary" @click="submitExpand">确认扩容</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showCoHostConfirmDialog" title="合作活动确认" width="500px">
      <div class="co-host-confirm-content">
        <div class="co-host-activity-title">{{ coHostConfirmForm.activityTitle }}</div>
        <div class="co-host-activity-desc">{{ coHostConfirmForm.activityDescription }}</div>
        <el-divider />
        <div class="co-host-info-row">
          <span class="label">主办方:</span>
          <span>{{ coHostConfirmForm.hostClubName }}</span>
        </div>
        <div class="co-host-info-row">
          <span class="label">活动地点:</span>
          <span>{{ coHostConfirmForm.location }}</span>
        </div>
        <div class="co-host-info-row">
          <span class="label">活动时间:</span>
          <span>{{ coHostConfirmForm.startTime?.split('T')[0] }} 至 {{ coHostConfirmForm.endTime?.split('T')[0] }}</span>
        </div>
        <div class="co-host-info-row">
          <span class="label">预算:</span>
          <span>¥{{ coHostConfirmForm.budget }}</span>
        </div>
      </div>
      <el-form :model="coHostConfirmForm" :rules="coHostConfirmRules" ref="coHostConfirmFormRef" label-width="80px" style="margin-top: 20px">
        <el-form-item label="确认结果">
          <el-radio-group v-model="coHostConfirmForm.status">
            <el-radio label="CONFIRMED">确认合作</el-radio>
            <el-radio label="REJECTED">拒绝</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="拒绝原因" v-if="coHostConfirmForm.status === 'REJECTED'" prop="reason" required>
          <el-input v-model="coHostConfirmForm.reason" type="textarea" placeholder="请输入拒绝原因" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCoHostConfirmDialog = false">取消</el-button>
        <el-button type="primary" @click="submitCoHostConfirm">确认</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showFeedbackDialog" title="活动反馈" width="500px">
      <el-form :model="feedbackForm" label-width="80px">
        <el-form-item label="评分" required>
          <el-rate v-model="feedbackForm.rating" :max="5" show-text />
        </el-form-item>
        <el-form-item label="反馈内容" required>
          <el-input 
            v-model="feedbackForm.feedback" 
            type="textarea" 
            :rows="4"
            placeholder="请输入您对本次活动的评价和建议..."
            maxlength="500"
            show-word-limit />
        </el-form-item>
      </el-form>
      <div v-if="feedbackResult.sentiment" class="feedback-result">
        <el-divider content-position="left">系统分析结果</el-divider>
        <div class="sentiment-result">
          <span>情绪标签：</span>
          <el-tag :type="sentimentType(feedbackResult.sentiment)" size="large">
            {{ sentimentText(feedbackResult.sentiment) }}
          </el-tag>
        </div>
        <div v-if="feedbackResult.tags && feedbackResult.tags.length > 0" class="tags-result">
          <span>命中关键词：</span>
          <el-tag 
            v-for="tag in feedbackResult.tags" 
            :key="tag"
            type="info"
            size="small"
            style="margin-right: 5px; margin-bottom: 5px;">
            {{ tag }}
          </el-tag>
        </div>
      </div>
      <template #footer>
        <el-button @click="showFeedbackDialog = false">取消</el-button>
        <el-button type="primary" @click="submitFeedback">提交反馈</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showStatsDialog" title="反馈情绪统计" width="800px">
      <div v-if="feedbackStats" class="feedback-stats">
        <el-row :gutter="20" class="stats-overview">
          <el-col :span="6">
            <el-card class="stat-card positive">
              <div class="stat-label">正面评价</div>
              <div class="stat-value">{{ feedbackStats.positiveCount }}</div>
              <div class="stat-percent">{{ feedbackStats.positivePercentage }}%</div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card class="stat-card neutral">
              <div class="stat-label">中性评价</div>
              <div class="stat-value">{{ feedbackStats.neutralCount }}</div>
              <div class="stat-percent">{{ feedbackStats.neutralPercentage }}%</div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card class="stat-card negative">
              <div class="stat-label">负面评价</div>
              <div class="stat-value">{{ feedbackStats.negativeCount }}</div>
              <div class="stat-percent">{{ feedbackStats.negativePercentage }}%</div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card class="stat-card avg">
              <div class="stat-label">平均评分</div>
              <div class="stat-value">{{ feedbackStats.averageRating || '-' }}</div>
              <div class="stat-percent">共 {{ feedbackStats.totalCount }} 条反馈</div>
            </el-card>
          </el-col>
        </el-row>

        <el-row :gutter="20" class="mt-20">
          <el-col :span="12">
            <el-card header="关键词分布">
              <div v-if="feedbackStats.tagFrequency && feedbackStats.tagFrequency.length > 0">
                <div v-for="item in feedbackStats.tagFrequency.slice(0, 10)" :key="item.tag" class="tag-frequency-item">
                  <span class="tag-name">{{ item.tag }}</span>
                  <el-progress 
                    :percentage="Math.min(100, Math.round(item.count / (feedbackStats.tagFrequency[0]?.count || 1) * 100))"
                    :show-text="false"
                    style="width: 150px; margin: 0 10px;" />
                  <span class="tag-count">{{ item.count }} 次</span>
                </div>
              </div>
              <el-empty v-else description="暂无关键词数据" :image-size="100" />
            </el-card>
          </el-col>
          <el-col :span="12">
            <el-card header="数据导出">
              <div class="export-section">
                <el-form :inline="true">
                  <el-form-item label="按情绪筛选">
                    <el-select v-model="exportFilter.sentiment" placeholder="全部" clearable>
                      <el-option label="正面" value="POSITIVE" />
                      <el-option label="中性" value="NEUTRAL" />
                      <el-option label="负面" value="NEGATIVE" />
                    </el-select>
                  </el-form-item>
                </el-form>
                <el-button type="primary" :loading="exporting" @click="handleExportFeedback">
                  <el-icon v-if="!exporting"><Download /></el-icon> 导出 Excel
                </el-button>
              </div>
              <el-divider />
              <div class="quick-filter-buttons">
                <el-button size="small" @click="exportFilter.sentiment = 'POSITIVE'">只看正面</el-button>
                <el-button size="small" @click="exportFilter.sentiment = 'NEUTRAL'">只看中性</el-button>
                <el-button size="small" @click="exportFilter.sentiment = 'NEGATIVE'">只看负面</el-button>
                <el-button size="small" @click="exportFilter.sentiment = ''">全部</el-button>
              </div>
            </el-card>
          </el-col>
        </el-row>

        <el-tabs v-model="statsActiveTab" class="mt-20">
          <el-tab-pane label="正面评价示例" name="positive">
            <div v-if="feedbackStats.positiveExamples && feedbackStats.positiveExamples.length > 0">
              <div v-for="item in feedbackStats.positiveExamples" :key="item.id" class="feedback-example">
                <div class="example-header">
                  <span class="user-name">{{ item.real_name || item.username }}</span>
                  <el-rate :model-value="item.rating" disabled size="small" />
                  <span v-if="item.tags && item.tags.length > 0" class="example-tags">
                    <el-tag v-for="tag in item.tags" :key="tag" type="success" size="small" style="margin-left: 3px;">
                      {{ tag }}
                    </el-tag>
                  </span>
                </div>
                <div class="example-content">{{ item.feedback }}</div>
              </div>
            </div>
            <el-empty v-else description="暂无正面评价" :image-size="100" />
          </el-tab-pane>
          <el-tab-pane label="中性评价示例" name="neutral">
            <div v-if="feedbackStats.neutralExamples && feedbackStats.neutralExamples.length > 0">
              <div v-for="item in feedbackStats.neutralExamples" :key="item.id" class="feedback-example">
                <div class="example-header">
                  <span class="user-name">{{ item.real_name || item.username }}</span>
                  <el-rate :model-value="item.rating" disabled size="small" />
                  <span v-if="item.tags && item.tags.length > 0" class="example-tags">
                    <el-tag v-for="tag in item.tags" :key="tag" type="info" size="small" style="margin-left: 3px;">
                      {{ tag }}
                    </el-tag>
                  </span>
                </div>
                <div class="example-content">{{ item.feedback }}</div>
              </div>
            </div>
            <el-empty v-else description="暂无中性评价" :image-size="100" />
          </el-tab-pane>
          <el-tab-pane label="负面评价示例" name="negative">
            <div v-if="feedbackStats.negativeExamples && feedbackStats.negativeExamples.length > 0">
              <div v-for="item in feedbackStats.negativeExamples" :key="item.id" class="feedback-example">
                <div class="example-header">
                  <span class="user-name">{{ item.real_name || item.username }}</span>
                  <el-rate :model-value="item.rating" disabled size="small" />
                  <span v-if="item.tags && item.tags.length > 0" class="example-tags">
                    <el-tag v-for="tag in item.tags" :key="tag" type="danger" size="small" style="margin-left: 3px;">
                      {{ tag }}
                    </el-tag>
                  </span>
                </div>
                <div class="example-content">{{ item.feedback }}</div>
              </div>
            </div>
            <el-empty v-else description="暂无负面评价" :image-size="100" />
          </el-tab-pane>
        </el-tabs>
      </div>
      <div v-else>
        <el-empty description="加载中..." />
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, computed } from 'vue'
import { Plus, Location, Calendar, InfoFilled, Download, List, Clock } from '@element-plus/icons-vue'
import request, { downloadFile, getFilenameFromContentDisposition } from '../utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '../store/user'

interface Activity {
  id: number
  title: string
  description: string
  process: string
  location: string
  startTime: string
  endTime: string
  budget: number
  maxCount: number
  clubId: number
  status: string
  poster?: string
  _detail?: {
    registeredCount: number
    waitlistCount: number
    isFull: boolean
    myStatus: string | null
    myPosition: number | null
    coHosts: any[]
    hostClub: any
  }
}

interface CalendarActivity {
  id: number
  title: string
  startTime: string
  endTime: string
  location: string
  clubId: number
  status: string
  _detail?: Activity['_detail']
}

const userStore = useUserStore()
const activities = ref<Activity[]>([])
const pendingCoHosts = ref<any[]>([])
const showAddDialog = ref(false)
const activeTab = ref('all')
const clubOptions = ref<any[]>([])

const viewMode = ref<'list' | 'calendar'>('list')
const calendarCurrentDate = ref(new Date())
const calendarActivities = ref<CalendarActivity[]>([])
const selectedDate = ref<Date | null>(null)
const selectedDateActivities = ref<CalendarActivity[]>([])
const showDayActivitiesDialog = ref(false)

const fetchActivities = async () => {
  try {
    const res: any = await request.get('/activities')
    activities.value = res
    for (const act of activities.value) {
      await loadActivityDetail(act)
    }
  } catch (err) {
    console.error('Failed to fetch activities:', err)
  }
}

const loadActivityDetail = async (act: Activity) => {
  try {
    const res: any = await request.get(`/activities/${act.id}/detail?userId=${userStore.userInfo?.id || ''}`)
    act._detail = res
  } catch (err) {
    console.error('Failed to load activity detail:', err)
  }
}

const fetchClubs = async () => {
  try {
    const res: any = await request.get('/clubs')
    const allClubs = res.records || res || []
    const myClubId = userStore.userInfo?.clubId
    clubOptions.value = allClubs.filter((club: any) => club.id !== myClubId && club.status === 'NORMAL')
  } catch (err) {
    console.error('Failed to fetch clubs:', err)
  }
}

const fetchPendingCoHosts = async () => {
  try {
    const res: any = await request.get('/activities/my-pending-co-hosts')
    pendingCoHosts.value = res || []
  } catch (err) {
    console.error('Failed to fetch pending co-hosts:', err)
  }
}

const formatDateTime = (date: Date): string => {
  const pad = (n: number) => n.toString().padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

const formatDateKey = (date: Date | string): string => {
  if (typeof date === 'string') {
    return date
  }
  const pad = (n: number) => n.toString().padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

const getMonthRange = (date: Date): { start: Date; end: Date } => {
  const year = date.getFullYear()
  const month = date.getMonth()
  const start = new Date(year, month, 1)
  const end = new Date(year, month + 1, 0, 23, 59, 59)
  return { start, end }
}

const fetchCalendarActivities = async () => {
  try {
    const { start, end } = getMonthRange(calendarCurrentDate.value)
    const params = new URLSearchParams({
      start: formatDateTime(start),
      end: formatDateTime(end)
    })
    const res: any = await request.get(`/activities/calendar?${params.toString()}`)
    calendarActivities.value = res || []
    for (const act of calendarActivities.value) {
      const listAct = activities.value.find(a => a.id === act.id)
      if (listAct) {
        act._detail = listAct._detail
      } else {
        await loadCalendarActivityDetail(act)
      }
    }
  } catch (err) {
    console.error('Failed to fetch calendar activities:', err)
  }
}

const loadCalendarActivityDetail = async (act: CalendarActivity) => {
  try {
    const res: any = await request.get(`/activities/${act.id}/detail?userId=${userStore.userInfo?.id || ''}`)
    act._detail = res
  } catch (err) {
    console.error('Failed to load calendar activity detail:', err)
  }
}

const isActivityOnDate = (act: CalendarActivity, dateStr: string): boolean => {
  const parseDate = (s: string): Date => {
    const clean = s.replace('T', ' ').substring(0, 19)
    return new Date(clean.replace(/-/g, '/'))
  }
  const actStart = parseDate(act.startTime)
  const actEnd = parseDate(act.endTime)
  const targetDate = new Date(dateStr.replace(/-/g, '/'))
  targetDate.setHours(0, 0, 0, 0)
  const nextDay = new Date(targetDate)
  nextDay.setDate(nextDay.getDate() + 1)

  const startOfDay = targetDate.getTime()
  const endOfDay = nextDay.getTime() - 1

  return actStart.getTime() <= endOfDay && actEnd.getTime() >= startOfDay
}

const getActivitiesForDate = (dateStr: string): CalendarActivity[] => {
  return calendarActivities.value.filter(act => isActivityOnDate(act, dateStr))
}

const selectedDateTitle = computed(() => {
  if (!selectedDate.value) return ''
  const d = selectedDate.value
  return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日 活动列表`
})

const isToday = (dateStr: string): boolean => {
  const today = new Date()
  return formatDateKey(today) === dateStr
}

const truncateText = (text: string, maxLen: number): string => {
  if (!text) return ''
  return text.length > maxLen ? text.substring(0, maxLen) + '...' : text
}

const getCalendarActivityClass = (act: CalendarActivity): string => {
  if (act.status === 'FINISHED') return 'act-finished'
  return 'act-approved'
}

const handleViewModeChange = (mode: string) => {
  if (mode === 'calendar') {
    fetchCalendarActivities()
  }
}

const handleDateClick = (dayStr: string) => {
  const parts = dayStr.split('-')
  selectedDate.value = new Date(parseInt(parts[0]), parseInt(parts[1]) - 1, parseInt(parts[2]))
  selectedDateActivities.value = getActivitiesForDate(dayStr)
  showDayActivitiesDialog.value = true
}

const handleCalendarActivityClick = (act: CalendarActivity) => {
  const parts = (act.startTime.replace('T', ' ').substring(0, 10)).split('-')
  selectedDate.value = new Date(parseInt(parts[0]), parseInt(parts[1]) - 1, parseInt(parts[2]))
  selectedDateActivities.value = getActivitiesForDate(formatDateKey(selectedDate.value))
  showDayActivitiesDialog.value = true
}

const findActivityById = (id: number): Activity | undefined => {
  return activities.value.find(a => a.id === id)
}

const canAuditFromCalendar = (act: CalendarActivity): boolean => {
  const listAct = findActivityById(act.id)
  if (!listAct) return false
  return canAudit(listAct)
}

const handleAuditFromCalendar = (act: CalendarActivity) => {
  const listAct = findActivityById(act.id)
  if (listAct) {
    handleAudit(listAct)
  }
}

const handleRegisterFromCalendar = async (act: CalendarActivity) => {
  const listAct = findActivityById(act.id)
  if (listAct) {
    await handleRegister(listAct)
    await fetchCalendarActivities()
  }
}

const formatActivityTime = (act: CalendarActivity): string => {
  const parseTime = (s: string): string => {
    const clean = s.replace('T', ' ')
    if (clean.includes(' ')) {
      const [date, time] = clean.split(' ')
      const t = time.substring(0, 5)
      const [, m, d] = date.split('-')
      return `${m}-${d} ${t}`
    }
    return clean
  }
  return `${parseTime(act.startTime)} ~ ${parseTime(act.endTime)}`
}

watch(calendarCurrentDate, () => {
  if (viewMode.value === 'calendar') {
    fetchCalendarActivities()
  }
})

const statusType = (s: string) => {
  if (s === 'DRAFT_COCONFIRM') return 'info'
  if (s === 'PENDING_UNION') return 'warning'
  if (s === 'PENDING_SCHOOL') return 'warning'
  if (s === 'APPROVED') return 'success'
  if (s === 'REJECTED') return 'danger'
  return 'info'
}

const statusText = (s: string) => {
  const map: any = { 
    DRAFT_COCONFIRM: '合作确认中', 
    PENDING_UNION: '社联初审', 
    PENDING_SCHOOL: '学校终审', 
    APPROVED: '进行中', 
    REJECTED: '已驳回', 
    FINISHED: '已结束' 
  }
  return map[s] || s
}

const coHostStatusType = (s: string) => {
  if (s === 'PENDING') return 'warning'
  if (s === 'CONFIRMED') return 'success'
  if (s === 'REJECTED') return 'danger'
  return 'info'
}

const coHostStatusText = (s: string) => {
  const map: any = { PENDING: '待确认', CONFIRMED: '已确认', REJECTED: '已拒绝' }
  return map[s] || s
}

const myStatusType = (s: string) => {
  if (s === 'REGISTERED') return 'success'
  if (s === 'SIGNED_IN') return 'primary'
  if (s === 'WAITLIST') return 'warning'
  if (s === 'CANCELLED') return 'info'
  return 'info'
}

const myStatusText = (detail: any) => {
  const map: any = { 
    REGISTERED: '已报名', 
    SIGNED_IN: '已签到', 
    WAITLIST: `候补中 - 第${detail.myPosition}位`, 
    CANCELLED: '已取消' 
  }
  return map[detail.myStatus] || detail.myStatus
}

const canAudit = (act: any) => {
  if (act.status === 'PENDING_UNION' && userStore.role === 'UNION_ADMIN') return true;
  if (act.status === 'PENDING_SCHOOL' && userStore.role === 'ADMIN') return true;
  return false;
}

const canExpand = (act: any) => {
  return userStore.role === 'CLUB_LEADER' && act.clubId === userStore.userInfo?.clubId
}

const canEdit = (act: any) => {
  if (userStore.role === 'ADMIN' || userStore.role === 'UNION_ADMIN') return true
  if (userStore.role === 'CLUB_LEADER' && act.clubId === userStore.userInfo?.clubId) {
    return !['APPROVED', 'FINISHED'].includes(act.status)
  }
  return false
}

const canDelete = (act: any) => {
  if (userStore.role === 'ADMIN' || userStore.role === 'UNION_ADMIN') return true
  if (userStore.role === 'CLUB_LEADER' && act.clubId === userStore.userInfo?.clubId) {
    return !['APPROVED', 'FINISHED'].includes(act.status)
  }
  return false
}

const canConfirmCoHost = (act: any) => {
  if (userStore.role !== 'CLUB_LEADER') return false
  if (act.status !== 'DRAFT_COCONFIRM') return false
  if (!act._detail?.coHosts) return false
  const myClubId = userStore.userInfo?.clubId
  const myCoHost = act._detail.coHosts.find((co: any) => co.clubId === myClubId)
  return myCoHost && myCoHost.status === 'PENDING'
}

const handleRegister = async (act: any) => {
  try {
    const res: any = await request.post(`/activities/${act.id}/register?userId=${userStore.userInfo?.id}`)
    ElMessage.success(res.message || '操作成功')
    loadActivityDetail(act)
  } catch (err: any) {
    console.error('Registration failed:', err)
    ElMessage.error(err.message || '操作失败')
  }
}

const handleCancel = async (act: any) => {
  try {
    await ElMessageBox.confirm('确定要取消报名吗？取消后名额将自动让给候补给用户。', '确认取消', {
      type: 'warning'
    })
    const res: any = await request.post(`/activities/${act.id}/cancel?userId=${userStore.userInfo?.id}`)
    ElMessage.success(res.message || '取消报名成功')
    loadActivityDetail(act)
  } catch (err: any) {
    if (err !== 'cancel') {
      console.error('Cancel failed:', err)
      ElMessage.error(err.message || '操作失败')
    }
  }
}

const handleLeaveWaitlist = async (act: any) => {
  try {
    await ElMessageBox.confirm('确定要退出候补队列吗？', '确认退出', {
      type: 'warning'
    })
    const res: any = await request.post(`/activities/${act.id}/leave-waitlist?userId=${userStore.userInfo?.id}`)
    ElMessage.success(res.message || '已退出候补')
    loadActivityDetail(act)
  } catch (err: any) {
    if (err !== 'cancel') {
      console.error('Leave waitlist failed:', err)
      ElMessage.error(err.message || '操作失败')
    }
  }
}

const addForm = ref({ 
  title: '', 
  description: '', 
  process: '', 
  location: '', 
  startTime: '', 
  endTime: '', 
  maxCount: 50, 
  budget: 0,
  coHostClubIds: [] as number[]
})

const showBudgetWarningDialog = ref(false)
const budgetWarningInfo = ref<any>({})
const budgetRiskAcknowledged = ref(false)

const submitAdd = async () => {
  try {
    const requestData: any = {
      title: addForm.value.title,
      description: addForm.value.description,
      process: addForm.value.process,
      location: addForm.value.location,
      startTime: addForm.value.startTime,
      endTime: addForm.value.endTime,
      maxCount: addForm.value.maxCount,
      budget: addForm.value.budget
    }
    if (addForm.value.coHostClubIds && addForm.value.coHostClubIds.length > 0) {
      requestData.coHostClubIds = addForm.value.coHostClubIds
    }
    const res: any = await request.post('/activities', requestData)
    if (res && res.budgetWarning) {
      budgetWarningInfo.value = res
      budgetRiskAcknowledged.value = false
      showBudgetWarningDialog.value = true
      return
    }
    ElMessage.success('活动发起成功')
    showAddDialog.value = false
    fetchActivities()
  } catch (err) {
    console.error('Form submission failed:', err)
  }
}

const forceSubmitAdd = async () => {
  try {
    if (!budgetWarningInfo.value.budgetToken) {
      ElMessage.error('预算确认凭证已失效，请重新提交表单')
      showBudgetWarningDialog.value = false
      return
    }
    const requestData: any = {
      title: addForm.value.title,
      description: addForm.value.description,
      process: addForm.value.process,
      location: addForm.value.location,
      startTime: addForm.value.startTime,
      endTime: addForm.value.endTime,
      maxCount: addForm.value.maxCount,
      budget: addForm.value.budget
    }
    if (addForm.value.coHostClubIds && addForm.value.coHostClubIds.length > 0) {
      requestData.coHostClubIds = addForm.value.coHostClubIds
    }
    const res: any = await request.post(`/activities?budgetToken=${encodeURIComponent(budgetWarningInfo.value.budgetToken)}`, requestData)
    if (res && res.budgetWarning) {
      ElMessage.warning('预算情况已发生变化，请重新确认')
      budgetWarningInfo.value = res
      budgetRiskAcknowledged.value = false
      return
    }
    ElMessage.success('活动发起成功')
    showBudgetWarningDialog.value = false
    showAddDialog.value = false
    fetchActivities()
  } catch (err) {
    console.error('Force submit failed:', err)
  }
}

const showEditDialog = ref(false)
const editForm = ref<any>({})
const editingActivityId = ref(0)
const showBudgetWarningEditDialog = ref(false)

const handleEdit = (act: any) => {
  editingActivityId.value = act.id
  editForm.value = {
    title: act.title,
    description: act.description,
    process: act.process || '',
    location: act.location,
    startTime: act.startTime,
    endTime: act.endTime,
    maxCount: act.maxCount,
    budget: act.budget
  }
  showEditDialog.value = true
}

const submitEdit = async () => {
  try {
    const res: any = await request.put(`/activities/${editingActivityId.value}`, editForm.value)
    if (res && res.budgetWarning) {
      budgetWarningInfo.value = res
      budgetRiskAcknowledged.value = false
      showBudgetWarningEditDialog.value = true
      return
    }
    ElMessage.success('活动修改成功')
    showEditDialog.value = false
    fetchActivities()
  } catch (err) {
    console.error('Edit submission failed:', err)
  }
}

const forceSubmitEdit = async () => {
  try {
    if (!budgetWarningInfo.value.budgetToken) {
      ElMessage.error('预算确认凭证已失效，请重新提交表单')
      showBudgetWarningEditDialog.value = false
      return
    }
    const res: any = await request.put(
      `/activities/${editingActivityId.value}?budgetToken=${encodeURIComponent(budgetWarningInfo.value.budgetToken)}`,
      editForm.value
    )
    if (res && res.budgetWarning) {
      ElMessage.warning('预算情况已发生变化，请重新确认')
      budgetWarningInfo.value = res
      budgetRiskAcknowledged.value = false
      return
    }
    ElMessage.success('活动修改成功')
    showBudgetWarningEditDialog.value = false
    showEditDialog.value = false
    fetchActivities()
  } catch (err) {
    console.error('Force edit failed:', err)
  }
}

const handleDelete = async (act: any) => {
  try {
    await ElMessageBox.confirm('确定要删除该活动吗？删除后预算将回滚。', '确认删除', {
      type: 'warning'
    })
    await request.delete(`/activities/${act.id}`)
    ElMessage.success('活动已删除')
    fetchActivities()
  } catch (err: any) {
    if (err !== 'cancel') {
      console.error('Delete failed:', err)
      ElMessage.error(err.message || '删除失败')
    }
  }
}

const showAuditDialog = ref(false)
const currentAuditId = ref(0)
const auditForm = ref({ status: 'APPROVED', reason: '' })

const handleAudit = (act: any) => {
  currentAuditId.value = act.id
  auditForm.value = { status: 'APPROVED', reason: '' }
  showAuditDialog.value = true
}

const submitAudit = async () => {
  try {
    await request.post(`/activities/${currentAuditId.value}/audit`, auditForm.value)
    ElMessage.success('审核完成')
    showAuditDialog.value = false
    fetchActivities()
  } catch (err) {
    console.error('Audit failed:', err)
  }
}

const showExpandDialog = ref(false)
const expandForm = ref({
  activityId: 0,
  activityTitle: '',
  currentMaxCount: 0,
  registeredCount: 0,
  waitlistCount: 0,
  newMaxCount: 0
})

const handleExpand = (act: any) => {
  expandForm.value = {
    activityId: act.id,
    activityTitle: act.title,
    currentMaxCount: act.maxCount,
    registeredCount: act._detail?.registeredCount || 0,
    waitlistCount: act._detail?.waitlistCount || 0,
    newMaxCount: act.maxCount + 1
  }
  showExpandDialog.value = true
}

const submitExpand = async () => {
  try {
    const res: any = await request.post(`/activities/${expandForm.value.activityId}/expand`, {
      newMaxCount: expandForm.value.newMaxCount
    })
    ElMessage.success(`扩容成功！已自动递补 ${res.promotedCount} 名候补给用户`)
    showExpandDialog.value = false
    fetchActivities()
  } catch (err: any) {
    console.error('Expand failed:', err)
    ElMessage.error(err.message || '扩容失败')
  }
}

const handleFinish = async (id: number) => {
  try {
    await request.post(`/activities/${id}/finish`)
    ElMessage.success('活动已结束')
    fetchActivities()
  } catch (err) {
    console.error('Finish activity failed:', err)
  }
}

const showFeedbackDialog = ref(false)
const feedbackForm = ref({
  activityId: 0,
  rating: 5,
  feedback: ''
})
const feedbackResult = ref({
  sentiment: '',
  tags: [] as string[]
})

const showStatsDialog = ref(false)
const statsActiveTab = ref('positive')
const feedbackStats = ref<any>(null)
const exporting = ref(false)
const exportFilter = ref({
  sentiment: '',
  activityId: 0
})

const canFeedback = (act: any) => {
  if (!['REGISTERED', 'SIGNED_IN'].includes(act._detail?.myStatus)) return false
  return act.status === 'FINISHED' || act.status === 'APPROVED'
}

const canViewStats = (act: any) => {
  if (userStore.role === 'ADMIN' || userStore.role === 'UNION_ADMIN') return true
  if (userStore.role === 'CLUB_LEADER' && act.clubId === userStore.userInfo?.clubId) return true
  return false
}

const sentimentType = (s: string) => {
  if (s === 'POSITIVE') return 'success'
  if (s === 'NEGATIVE') return 'danger'
  return 'info'
}

const sentimentText = (s: string) => {
  const map: any = { POSITIVE: '正面', NEUTRAL: '中性', NEGATIVE: '负面' }
  return map[s] || s
}

const handleFeedback = (act: any) => {
  feedbackForm.value = {
    activityId: act.id,
    rating: 5,
    feedback: ''
  }
  feedbackResult.value = { sentiment: '', tags: [] }
  showFeedbackDialog.value = true
}

const submitFeedback = async () => {
  if (!feedbackForm.value.feedback || feedbackForm.value.feedback.trim() === '') {
    ElMessage.warning('请输入反馈内容')
    return
  }
  try {
    const res: any = await request.post(`/activities/${feedbackForm.value.activityId}/feedback`, {
      userId: userStore.userInfo?.id,
      rating: feedbackForm.value.rating,
      feedback: feedbackForm.value.feedback
    })
    if (res && res.sentiment) {
      feedbackResult.value = res
      ElMessage.success('反馈提交成功！系统已自动分析情绪标签')
      setTimeout(() => {
        showFeedbackDialog.value = false
      }, 2000)
    } else {
      ElMessage.success('反馈提交成功')
      showFeedbackDialog.value = false
    }
    loadActivityDetail(activities.value.find((a: any) => a.id === feedbackForm.value.activityId)!)
  } catch (err: any) {
    console.error('Feedback submission failed:', err)
    ElMessage.error(err.message || '提交失败')
  }
}

const handleViewStats = async (act: any) => {
  showStatsDialog.value = true
  feedbackStats.value = null
  exportFilter.value = { sentiment: '', activityId: act.id }
  try {
    const res: any = await request.get(`/activities/${act.id}/feedback-stats`)
    feedbackStats.value = res
  } catch (err: any) {
    console.error('Failed to load feedback stats:', err)
    ElMessage.error('加载统计数据失败')
  }
}

const handleExportFeedback = async () => {
  if (exporting.value) return
  exporting.value = true
  try {
    const params = new URLSearchParams()
    params.append('activityId', String(exportFilter.value.activityId))
    if (exportFilter.value.sentiment) {
      params.append('sentiment', exportFilter.value.sentiment)
    }
    const resp: any = await request.get(`/admin/export/feedback?${params.toString()}`, {
      responseType: 'blob'
    })
    const disposition = resp.headers?.['content-disposition'] || null
    const filename = getFilenameFromContentDisposition(disposition, '活动反馈.xlsx')
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

const showCoHostConfirmDialog = ref(false)
const coHostConfirmFormRef = ref()
const coHostConfirmForm = ref({
  activityId: 0,
  coHostId: 0,
  activityTitle: '',
  activityDescription: '',
  hostClubName: '',
  location: '',
  startTime: '',
  endTime: '',
  budget: 0,
  status: 'CONFIRMED',
  reason: ''
})
const coHostConfirmRules = {
  reason: [
    { required: true, message: '请输入拒绝原因', trigger: 'blur' }
  ]
}

const handleCoHostConfirm = (act: any) => {
  const myClubId = userStore.userInfo?.clubId
  const myCoHost = act._detail?.coHosts?.find((co: any) => co.clubId === myClubId)
  if (!myCoHost) return

  coHostConfirmForm.value = {
    activityId: act.id,
    coHostId: myCoHost.id,
    activityTitle: act.title,
    activityDescription: act.description,
    hostClubName: act._detail?.hostClub?.name || '',
    location: act.location,
    startTime: act.startTime,
    endTime: act.endTime,
    budget: act.budget,
    status: 'CONFIRMED',
    reason: ''
  }
  showCoHostConfirmDialog.value = true
}

const handleConfirmFromList = (item: any) => {
  coHostConfirmForm.value = {
    activityId: item.activityId,
    coHostId: item.coHostId,
    activityTitle: item.activityTitle,
    activityDescription: item.activityDescription,
    hostClubName: item.hostClub?.name || '',
    location: item.location,
    startTime: item.startTime,
    endTime: item.endTime,
    budget: item.budget,
    status: 'CONFIRMED',
    reason: ''
  }
  showCoHostConfirmDialog.value = true
}

const handleRejectFromList = (item: any) => {
  coHostConfirmForm.value = {
    activityId: item.activityId,
    coHostId: item.coHostId,
    activityTitle: item.activityTitle,
    activityDescription: item.activityDescription,
    hostClubName: item.hostClub?.name || '',
    location: item.location,
    startTime: item.startTime,
    endTime: item.endTime,
    budget: item.budget,
    status: 'REJECTED',
    reason: ''
  }
  showCoHostConfirmDialog.value = true
}

const submitCoHostConfirm = async () => {
  try {
    if (coHostConfirmForm.value.status === 'REJECTED') {
      if (!coHostConfirmForm.value.reason || coHostConfirmForm.value.reason.trim() === '') {
        ElMessage.warning('请输入拒绝原因')
        return
      }
      await coHostConfirmFormRef.value.validateField('reason')
    }
    const { activityId, coHostId, status, reason } = coHostConfirmForm.value
    await request.post(`/activities/${activityId}/co-hosts/${coHostId}/confirm`, { status, reason })
    ElMessage.success('操作成功')
    showCoHostConfirmDialog.value = false
    fetchActivities()
    fetchPendingCoHosts()
  } catch (err: any) {
    if (err !== false) {
      console.error('Co-host confirm failed:', err)
      ElMessage.error(err.message || '操作失败')
    }
  }
}

onMounted(() => {
  fetchActivities()
  if (userStore.role === 'CLUB_LEADER') {
    fetchClubs()
    fetchPendingCoHosts()
  }
})
</script>

<style scoped>
.view-switch-wrapper {
  display: inline-flex;
  align-items: center;
  margin-left: auto;
}
.view-switch-wrapper :deep(.el-radio-button__inner) {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 8px 16px;
}
.view-switch-toolbar {
  display: flex;
  justify-content: flex-end;
  padding: 15px 20px;
}
.toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
}
.calendar-view {
  background: rgba(255, 255, 255, 0.9);
  border-radius: 8px;
  padding: 20px;
}
.calendar-view :deep(.el-calendar) {
  --el-calendar-border: 1px solid #ebeef5;
}
.calendar-view :deep(.el-calendar__header) {
  padding: 12px 20px;
  border-bottom: 1px solid var(--el-calendar-border);
}
.calendar-view :deep(.el-calendar__body) {
  padding: 0;
}
.calendar-cell {
  min-height: 100px;
  padding: 6px;
  cursor: pointer;
  transition: all 0.2s ease;
  border-radius: 4px;
  position: relative;
}
.calendar-cell:hover {
  background-color: #f5f7fa;
}
.calendar-cell.is-today {
  background-color: #ecf5ff;
}
.calendar-cell.is-selected {
  background-color: #409eff20;
  outline: 2px solid #409eff;
}
.cell-date {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 6px;
}
.calendar-view :deep(.el-calendar-table .el-calendar-day) {
  padding: 0;
  height: 120px;
}
.calendar-view :deep(.el-calendar-table .el-calendar-day:hover) {
  background-color: transparent;
}
.calendar-view :deep(.el-calendar-table td.is-selected .el-calendar-day) {
  background-color: transparent;
}
.calendar-view :deep(.el-calendar-table td.prev-month .cell-date),
.calendar-view :deep(.el-calendar-table td.next-month .cell-date) {
  color: #c0c4cc;
}
.cell-activities {
  display: flex;
  flex-direction: column;
  gap: 3px;
}
.cell-activity-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  padding: 2px 4px;
  border-radius: 3px;
  line-height: 1.4;
  white-space: nowrap;
  overflow: hidden;
}
.cell-activity-item.act-approved {
  background-color: #e1f3d8;
  color: #67c23a;
}
.cell-activity-item.act-finished {
  background-color: #f4f4f5;
  color: #909399;
}
.cell-activity-item .dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}
.cell-activity-item.act-approved .dot {
  background-color: #67c23a;
}
.cell-activity-item.act-finished .dot {
  background-color: #909399;
}
.cell-activity-item .act-title {
  overflow: hidden;
  text-overflow: ellipsis;
}
.cell-more {
  font-size: 11px;
  color: #909399;
  padding: 2px 4px;
}
.empty-day {
  padding: 40px 0;
}
.day-activities-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
  max-height: 60vh;
  overflow-y: auto;
  padding-right: 5px;
}
.day-activity-card {
  padding: 15px;
  border-radius: 8px;
  border: 1px solid #ebeef5;
}
.day-activity-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}
.day-activity-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}
.day-activity-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 15px;
  margin-bottom: 12px;
  font-size: 13px;
  color: #606266;
}
.day-activity-meta .meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
}
.day-activity-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.act-card {
  height: auto;
  min-height: 320px;
}
.card-cover {
  width: 100%;
  height: 120px;
  overflow: hidden;
}
.card-header {
  padding: 15px;
  border-bottom: 1px solid rgba(0,0,0,0.05);
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.card-header h3 {
  font-size: 16px;
  margin: 0;
}
.card-body {
  padding: 15px;
}
.desc {
  font-size: 14px;
  color: #666;
  height: 40px;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}
.info {
  margin-top: 15px;
  display: flex;
  gap: 15px;
  font-size: 12px;
  color: #999;
}
.club-info {
  margin-top: 10px;
  font-size: 13px;
  color: #606266;
}
.co-host-info {
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
}
.budget-info {
  margin-top: 10px;
  font-size: 13px;
  color: #e6a23c;
  font-weight: 500;
}
.enrollment-info {
  margin-top: 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  color: #666;
}
.enrollment-count {
  font-weight: 500;
}
.waitlist-count {
  color: #e6a23c;
  margin-left: 5px;
}
.my-status {
  margin-top: 10px;
}
.actions {
  margin-top: 15px;
  text-align: right;
  display: flex;
  gap: 8px;
  justify-content: flex-end;
  flex-wrap: wrap;
}
.mb-20 {
  margin-bottom: 20px;
}
.toolbar {
  padding: 15px 20px;
}
.expand-hint {
  margin-top: 10px;
  padding: 10px;
  background-color: #ecf5ff;
  border: 1px solid #d9ecff;
  border-radius: 4px;
  color: #409eff;
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 6px;
}
.form-tip {
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
}
.tab-badge {
  margin-left: 8px;
}
.co-host-confirm-content {
  padding: 10px 0;
}
.co-host-activity-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 10px;
}
.co-host-activity-desc {
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
  margin-bottom: 15px;
}
.co-host-info-row {
  display: flex;
  margin-bottom: 10px;
  font-size: 14px;
}
.co-host-info-row .label {
  width: 80px;
  color: #909399;
  flex-shrink: 0;
}
.feedback-result {
  margin-top: 15px;
  padding: 15px;
  background: #f5f7fa;
  border-radius: 4px;
}
.sentiment-result {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
  font-size: 14px;
}
.tags-result {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  font-size: 14px;
}
.feedback-stats {
  padding: 10px 0;
}
.stats-overview .stat-card {
  text-align: center;
  border: none;
  transition: transform 0.2s;
}
.stats-overview .stat-card:hover {
  transform: translateY(-2px);
}
.stats-overview .stat-card.positive {
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f7fa 100%);
}
.stats-overview .stat-card.neutral {
  background: linear-gradient(135deg, #fafafa 0%, #f5f5f5 100%);
}
.stats-overview .stat-card.negative {
  background: linear-gradient(135deg, #fff5f5 0%, #ffebee 100%);
}
.stats-overview .stat-card.avg {
  background: linear-gradient(135deg, #fff8e1 0%, #ffecb3 100%);
}
.stats-overview .stat-label {
  font-size: 13px;
  color: #909399;
  margin-bottom: 8px;
}
.stats-overview .stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
}
.stats-overview .stat-percent {
  font-size: 12px;
  color: #909399;
  margin-top: 5px;
}
.stats-overview .positive .stat-value {
  color: #67c23a;
}
.stats-overview .neutral .stat-value {
  color: #909399;
}
.stats-overview .negative .stat-value {
  color: #f56c6c;
}
.stats-overview .avg .stat-value {
  color: #e6a23c;
}
.tag-frequency-item {
  display: flex;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
}
.tag-frequency-item:last-child {
  border-bottom: none;
}
.tag-name {
  width: 80px;
  font-size: 13px;
  color: #606266;
  font-weight: 500;
}
.tag-count {
  font-size: 12px;
  color: #909399;
  min-width: 50px;
}
.export-section {
  display: flex;
  flex-direction: column;
  gap: 15px;
}
.quick-filter-buttons {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.feedback-example {
  padding: 15px;
  margin-bottom: 10px;
  background: #fafafa;
  border-radius: 4px;
  border-left: 3px solid #dcdfe6;
}
.feedback-example:nth-child(odd) {
  background: #f5f7fa;
}
.example-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
  flex-wrap: wrap;
}
.example-header .user-name {
  font-weight: 600;
  color: #303133;
  font-size: 14px;
}
.example-tags {
  margin-left: auto;
}
.example-content {
  color: #606266;
  font-size: 13px;
  line-height: 1.6;
}
.mt-20 {
  margin-top: 20px;
}
</style>
