import request from '../utils/request'

export interface Badge {
  id: number
  clubId: number
  clubName?: string
  name: string
  iconUrl?: string
  description?: string
  isPublic?: number
  createTime?: string
  recipientCount?: number
}

export interface UserBadge {
  userBadgeId: number
  badgeId: number
  name: string
  iconUrl?: string
  description?: string
  clubId: number
  clubName?: string
  isPublic?: number
  grantedTime?: string
  grantedByName?: string
}

export const badgeApi = {
  getBadgeList: (params?: { clubId?: number; isPublic?: boolean }) =>
    request.get('/badges', { params }),

  getBadgeDetail: (id: number) =>
    request.get(`/badges/${id}`),

  getClubPublicBadges: (clubId: number) =>
    request.get(`/badges/club/${clubId}/public`),

  getUserBadges: (userId: number) =>
    request.get(`/badges/user/${userId}`),

  getMyBadges: () =>
    request.get('/badges/mine'),

  getBadgeRecipients: (badgeId: number) =>
    request.get(`/badges/${badgeId}/recipients`),

  createBadge: (data: {
    clubId: number
    name: string
    iconUrl?: string
    description?: string
    isPublic?: number
  }) => request.post('/badges', data),

  updateBadge: (id: number, data: {
    name?: string
    iconUrl?: string
    description?: string
    isPublic?: number
  }) => request.put(`/badges/${id}`, data),

  deleteBadge: (id: number) =>
    request.delete(`/badges/${id}`),

  grantBadge: (data: { badgeId: number; userId: number }) =>
    request.post('/badges/grant', data),

  revokeBadge: (userBadgeId: number, data?: { revokeReason?: string }) =>
    request.post(`/badges/user-badges/${userBadgeId}/revoke`, data)
}
