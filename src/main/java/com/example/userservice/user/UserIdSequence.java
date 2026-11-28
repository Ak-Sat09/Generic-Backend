package com.example.userservice.user;

import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Numeric user ids from an atomic counter in MongoDB, so a restart or a second server can
 * never hand out the same id. The first call lifts the counter above the largest existing id.
 */
@Component
public class UserIdSequence {

    static final String COUNTERS = "counters";
    static final String KEY = "app_users";

    private final MongoTemplate mongo;
    private final AtomicBoolean seeded = new AtomicBoolean();

    public UserIdSequence(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    public long next() {
        if (seeded.compareAndSet(false, true)) {
            seedFromExistingUsers();
        }
        Document counter = mongo.findAndModify(
                Query.query(Criteria.where("_id").is(KEY)),
                new Update().inc("value", 1L),
                FindAndModifyOptions.options().upsert(true).returnNew(true),
                Document.class,
                COUNTERS);
        return ((Number) counter.get("value")).longValue();
    }

    private void seedFromExistingUsers() {
        Query newest = new Query().with(Sort.by(Sort.Direction.DESC, "_id")).limit(1);
        newest.fields().include("_id");
        User latest = mongo.findOne(newest, User.class);
        long highest = latest == null || latest.getId() == null ? 0L : latest.getId();
        mongo.upsert(Query.query(Criteria.where("_id").is(KEY)), new Update().max("value", highest), COUNTERS);
    }
}
