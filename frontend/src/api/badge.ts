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
  grants?: BadgeGrant[]
}

export interface BadgeGrant {
  userBadgeId: number
  userId: number
  userName: string
  userAvatar?: string
  grantedTime?: string
  grantedByName?: string
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
  getPublicBadgeList: (params?: { clubId?: number }) =>
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

  getManagedBadges: () =>
    request.get('/badges/managed'),

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
    request.post(`/badges/user-badges/${userBadgeId}/revoke`, data),

  getClubMembers: (clubId: number) =>
    request.get(`/clubs/${clubId}/members`)
}
