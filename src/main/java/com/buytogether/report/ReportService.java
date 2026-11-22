package com.buytogether.report;

import com.buytogether.common.error.BusinessException;
import com.buytogether.common.error.ErrorCode;
import com.buytogether.common.text.Text;
import com.buytogether.group.GroupQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.Clock;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final GroupReportRepository reports;
    private final GroupQueryService groupQueries;
    private final Clock clock;

    public String report(String groupId, String reporterId, ReportReason reason, String details) {
        groupQueries.view(groupId);
        try {
            return reports.insert(GroupReport.builder()
                    .groupId(groupId)
                    .reporterId(reporterId)
                    .reason(reason)
                    .details(Text.clean(details))
                    .createdAt(clock.instant())
                    .build()).getId();
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ErrorCode.ALREADY_REPORTED);
        }
    }
}
