import request from '../utils/request'

export const topicApi = {
    getTopics: (type: string = 'IN_CLUB') =>
        request.get(`/topics?type=${type}`),

    getPendingTopics: () => request.get('/topics/pending'),

    publishTopic: (data: { title: string; content: string; type: string; clubId?: number }) =>
        request.post('/topics', data),

    auditTopic: (id: number, status: string) =>
        request.post(`/topics/${id}/audit`, { status }),

    interactTopic: (id: number, type: string) =>
        request.post(`/topics/${id}/interact`, { type }),

    getTopicById: (id: number) =>
        request.get(`/topics/${id}`),

    getComments: (topicId: number) =>
        request.get(`/topics/${topicId}/comments`),

    publishComment: (topicId: number, content: string, replyId?: number) =>
        request.post(`/topics/${topicId}/comments`, { content, replyId })
}
