package com.buytogether.report;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("group_reports")
@CompoundIndex(name = "group_reporter_uq", def = "{'groupId': 1, 'reporterId': 1}", unique = true)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class GroupReport {

    @Id
    private String id;

    private String groupId;
    private String reporterId;
    private ReportReason reason;
    private String details;
    private Instant createdAt;
}
