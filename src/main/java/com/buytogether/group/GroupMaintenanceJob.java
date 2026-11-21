package com.buytogether.group;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Nightly housekeeping: expire groups past their deadline, then delete old finished ones.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class GroupMaintenanceJob {

    private final GroupService groupService;
    private final GroupPurgeService purgeService;

    @Scheduled(cron = "${buy-together.groups.maintenance-cron}", zone = "Asia/Kolkata")
    void run() {
        long expired = groupService.expireOverdueGroups();
        int purged = purgeService.purgeFinishedGroups();
        if (expired > 0 || purged > 0) {
            log.info("Group maintenance: {} expired, {} deleted", expired, purged);
        }
    }
}
