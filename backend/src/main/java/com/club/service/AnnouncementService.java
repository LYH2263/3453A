package com.club.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.club.common.Result;
import com.club.entity.Announcement;

public interface AnnouncementService extends IService<Announcement> {
    Result<?> publishAnnouncement(Announcement announcement);
    Result<?> getAnnouncements();
    Result<?> deleteAnnouncement(Integer id);
}
