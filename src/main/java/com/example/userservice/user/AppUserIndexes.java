package com.example.userservice.user;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * Email and phone are each unique only when set: phone-only accounts have no email and the other way
 * round, and a plain unique index treats every missing value as the same null. Replaces the older plain
 * unique index on email.
 */
@Component
@ConditionalOnProperty(name = "spring.data.mongodb.auto-index-creation", havingValue = "true")
class AppUserIndexes {

    private static final Logger log = LoggerFactory.getLogger(AppUserIndexes.class);

    private final MongoTemplate mongo;

    AppUserIndexes(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    @EventListener(ApplicationReadyEvent.class)
    void ensure() {
        try {
            IndexOperations indexes = mongo.indexOps(User.class);
            List<IndexInfo> existing = indexes.getIndexInfo();
            replace(indexes, existing, "email", "app_users_email_unique");
            replace(indexes, existing, "phone", "app_users_phone_unique");
        } catch (DataAccessException e) {
            log.error("Could not set up app_users indexes: {}", e.getMessage());
        }
    }

    private void replace(IndexOperations indexes, List<IndexInfo> existing, String field, String name) {
        for (IndexInfo index : existing) {
            if (index.isIndexForFields(List.of(field)) && !index.getName().equals(name)) {
                log.info("Replacing index {} on app_users", index.getName());
                indexes.dropIndex(index.getName());
            }
        }
        indexes.ensureIndex(new Index().on(field, Sort.Direction.ASC).named(name).unique()
                .partial(PartialIndexFilter.of(new Document(field, new Document("$type", "string")))));
    }
}
