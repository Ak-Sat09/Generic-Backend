package com.example.userservice.account;

import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

@Component
class MongoAccountTokenStore implements AccountTokenStore {

    private final MongoTemplate mongo;

    MongoAccountTokenStore(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    @Override
    public void save(AccountToken token) {
        mongo.insert(token);
    }

    @Override
    public Optional<AccountToken> consume(String tokenHash, AccountToken.Purpose purpose, Instant now) {
        Query usable = Query.query(Criteria.where("tokenHash").is(tokenHash).and("purpose").is(purpose)
                .and("usedAt").is(null).and("expiresAt").gt(now));
        return Optional.ofNullable(mongo.findAndModify(usable, new Update().set("usedAt", now),
                FindAndModifyOptions.options().returnNew(true), AccountToken.class));
    }

    @Override
    public void retireAll(Long userId, AccountToken.Purpose purpose, Instant now) {
        mongo.updateMulti(Query.query(Criteria.where("userId").is(userId).and("purpose").is(purpose).and("usedAt").is(null)),
                new Update().set("usedAt", now), AccountToken.class);
    }
}
