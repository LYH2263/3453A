package com.club.controller;

import com.club.common.Result;
import com.club.entity.Announcement;
import com.club.service.AnnouncementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/announcements")
public class AnnouncementController {

    @Autowired
    private AnnouncementService announcementService;

    @GetMapping
    public Result<?> getAnnouncements() {
        return announcementService.getAnnouncements();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'UNION_ADMIN', 'CLUB_LEADER')")
    public Result<?> publishAnnouncement(@RequestBody Announcement announcement) {
        return announcementService.publishAnnouncement(announcement);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'UNION_ADMIN', 'CLUB_LEADER')")
    public Result<?> deleteAnnouncement(@PathVariable Integer id) {
        return announcementService.deleteAnnouncement(id);
    }
}
