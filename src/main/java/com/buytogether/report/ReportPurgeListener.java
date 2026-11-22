package com.buytogether.report;

import com.buytogether.group.GroupsPurgedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class ReportPurgeListener {

    private final GroupReportRepository reports;

    @EventListener
    void onGroupsPurged(GroupsPurgedEvent event) {
        reports.deleteByGroupIdIn(event.groupIds());
    }
}
