package com.example.userservice.token;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

@Component
class MongoRefreshTokenStore implements RefreshTokenStore {

    private final MongoTemplate mongo;

    MongoRefreshTokenStore(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    @Override
    public void save(RefreshToken token) {
        mongo.insert(token);
    }

    @Override
    public Optional<RefreshToken> findByHash(String tokenHash) {
        return Optional.ofNullable(mongo.findOne(Query.query(Criteria.where("tokenHash").is(tokenHash)), RefreshToken.class));
    }

    @Override
    public boolean markUsed(String id, Instant at) {
        Query fresh = Query.query(Criteria.where("_id").is(id).and("usedAt").is(null).and("revokedAt").is(null));
        return mongo.updateFirst(fresh, new Update().set("usedAt", at), RefreshToken.class).getModifiedCount() == 1;
    }

    @Override
    public void revokeFamily(String familyId, Instant at) {
        mongo.updateMulti(Query.query(Criteria.where("familyId").is(familyId).and("revokedAt").is(null)),
                new Update().set("revokedAt", at), RefreshToken.class);
    }

    @Override
    public void revokeAllForUser(Long userId, Instant at) {
        mongo.updateMulti(Query.query(Criteria.where("userId").is(userId).and("revokedAt").is(null)),
                new Update().set("revokedAt", at), RefreshToken.class);
    }
}
