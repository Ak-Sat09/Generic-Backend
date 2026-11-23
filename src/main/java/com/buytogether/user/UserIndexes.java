package com.buytogether.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexInfo;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.index.PartialIndexFilter;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * A user has a phone, an email or both, so each is unique only when present.
 * A plain unique index treats every missing value as the same null and would let only
 * one email-only user exist, so these are partial indexes. Runs where Spring Data
 * would create indexes, and replaces the older plain unique index on phone.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.data.mongodb.auto-index-creation", havingValue = "true")
class UserIndexes {

    static final String PHONE_INDEX = "phone_unique";
    static final String EMAIL_INDEX = "email_unique";

    private final MongoTemplate mongo;

    @EventListener(ApplicationReadyEvent.class)
    void ensure() {
        try {
            IndexOperations indexes = mongo.indexOps(User.class);
            List<IndexInfo> existing = indexes.getIndexInfo();
            replace(indexes, existing, "phone", PHONE_INDEX);
            replace(indexes, existing, "email", EMAIL_INDEX);
        } catch (DataAccessException e) {
            // keep serving: logins still work, only the uniqueness guarantee is missing until the next start
            log.error("Could not set up bt_users indexes: {}", e.getMessage());
        }
    }

    private void replace(IndexOperations indexes, List<IndexInfo> existing, String field, String name) {
        for (IndexInfo index : existing) {
            if (index.isIndexForFields(List.of(field)) && !index.getName().equals(name)) {
                log.info("Dropping old index {} on bt_users", index.getName());
                indexes.dropIndex(index.getName());
            }
        }
        indexes.ensureIndex(new Index()
                .on(field, Sort.Direction.ASC)
                .named(name)
                .unique()
                .partial(PartialIndexFilter.of(new Document(field, new Document("$type", "string")))));
    }
}
