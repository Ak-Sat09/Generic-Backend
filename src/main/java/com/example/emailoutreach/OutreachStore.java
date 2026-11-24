package com.example.emailoutreach;

import com.mongodb.client.MongoClient;
import org.bson.Document;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The outreach data lives in its own database (email_outreach by default), separate from the
 * other modules, so this uses a private MongoTemplate on the shared client instead of a bean.
 */
@Component
class OutreachStore {

    static final String CONTACTS = "email_contacts";
    static final String SYNC_STATE = "sync_state";
    static final String REPLY_SYNC_ID = "reply-sync";
    static final String BACKFILL_ID = "backfill";
    static final String RESPONDED = "RESPONDED";

    private final MongoTemplate mongo;

    OutreachStore(MongoClient client, EmailOutreachProperties properties) {
        this.mongo = new MongoTemplate(client, properties.database());
    }

    Set<String> knownEmails(Collection<String> emails) {
        Set<String> known = new HashSet<>();
        if (emails.isEmpty()) {
            return known;
        }
        Query query = Query.query(Criteria.where("email").in(emails));
        query.fields().include("email");
        for (Document contact : mongo.find(query, Document.class, CONTACTS)) {
            known.add(contact.getString("email"));
        }
        return known;
    }

    /** Marks each contact RESPONDED with its first reply time. Contacts already RESPONDED are left as they are. */
    int markResponded(Map<String, Instant> firstReplyByEmail, Instant now) {
        if (firstReplyByEmail.isEmpty()) {
            return 0;
        }
        BulkOperations bulk = mongo.bulkOps(BulkOperations.BulkMode.UNORDERED, CONTACTS);
        firstReplyByEmail.forEach((email, repliedAt) -> bulk.updateOne(
                Query.query(Criteria.where("email").is(email).and("status").ne(RESPONDED)),
                new Update().set("status", RESPONDED)
                        .set("respondedAt", Date.from(repliedAt))
                        .set("updatedAt", Date.from(now))));
        return bulk.execute().getModifiedCount();
    }

    boolean backfillRunning() {
        Document backfill = mongo.findById(BACKFILL_ID, Document.class, SYNC_STATE);
        return backfill != null && "RUNNING".equals(backfill.getString("status"));
    }

    Optional<Instant> lastSuccessAt() {
        Document state = mongo.findById(REPLY_SYNC_ID, Document.class, SYNC_STATE);
        Date last = state == null ? null : state.getDate("lastSuccessAt");
        return Optional.ofNullable(last).map(Date::toInstant);
    }

    void recordSuccess(Instant at, int scanned, int marked) {
        mongo.upsert(Query.query(Criteria.where("_id").is(REPLY_SYNC_ID)),
                new Update().set("lastRunAt", Date.from(at))
                        .set("lastSuccessAt", Date.from(at))
                        .set("lastScanned", scanned)
                        .set("lastMarked", marked)
                        .unset("lastError"),
                SYNC_STATE);
    }

    void recordFailure(Instant at, String error) {
        mongo.upsert(Query.query(Criteria.where("_id").is(REPLY_SYNC_ID)),
                new Update().set("lastRunAt", Date.from(at)).set("lastError", error),
                SYNC_STATE);
    }
}
