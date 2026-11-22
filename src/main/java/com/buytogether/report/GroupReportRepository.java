package com.buytogether.report;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;

public interface GroupReportRepository extends MongoRepository<GroupReport, String> {

    long deleteByGroupIdIn(Collection<String> groupIds);
}
