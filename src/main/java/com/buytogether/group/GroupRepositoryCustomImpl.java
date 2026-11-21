package com.buytogether.group;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.springframework.data.mongodb.core.query.Criteria.where;

@RequiredArgsConstructor
class GroupRepositoryCustomImpl implements GroupRepositoryCustom {

    private static final String ID = "id";
    private static final String STATUS = "status";
    private static final String MEMBER_COUNT = "memberCount";
    private static final String UPDATED_AT = "updatedAt";

    private final MongoTemplate mongo;

    @Override
    public Optional<BuyingGroup> takeSeat(String groupId, int targetSize, Instant now) {
        Query query = new Query(where(ID).is(groupId)
                .and(STATUS).is(GroupStatus.OPEN)
                .and(MEMBER_COUNT).lt(targetSize));
        Update update = new Update().inc(MEMBER_COUNT, 1).set(UPDATED_AT, now);
        return Optional.ofNullable(mongo.findAndModify(query, update, returnNew(), BuyingGroup.class));
    }

    @Override
    public Optional<BuyingGroup> releaseSeat(String groupId, Instant now) {
        Query query = new Query(where(ID).is(groupId)
                .and(STATUS).in(GroupStatus.ACTIVE)
                .and(MEMBER_COUNT).gt(1));
        Update update = new Update()
                .inc(MEMBER_COUNT, -1)
                .set(STATUS, GroupStatus.OPEN)
                .set(UPDATED_AT, now);
        return Optional.ofNullable(mongo.findAndModify(query, update, returnNew(), BuyingGroup.class));
    }

    @Override
    public boolean markFullIfTargetReached(String groupId, int targetSize, Instant now) {
        Query query = new Query(where(ID).is(groupId)
                .and(STATUS).is(GroupStatus.OPEN)
                .and(MEMBER_COUNT).gte(targetSize));
        Update update = new Update().set(STATUS, GroupStatus.FULL).set(UPDATED_AT, now);
        return mongo.updateFirst(query, update, BuyingGroup.class).getModifiedCount() > 0;
    }

    @Override
    public boolean finish(String groupId, GroupStatus finalStatus, Instant now) {
        if (!GroupStatus.FINISHED.contains(finalStatus)) {
            throw new IllegalArgumentException(finalStatus + " is not a final status");
        }
        Query query = new Query(where(ID).is(groupId).and(STATUS).in(GroupStatus.ACTIVE));
        Update update = new Update().set(STATUS, finalStatus).set(UPDATED_AT, now);
        return mongo.updateFirst(query, update, BuyingGroup.class).getModifiedCount() > 0;
    }

    @Override
    public long expireDeadlinesBefore(LocalDate day, Instant now) {
        Query query = new Query(where(STATUS).in(GroupStatus.ACTIVE).and("deadline").lt(day));
        Update update = new Update().set(STATUS, GroupStatus.EXPIRED).set(UPDATED_AT, now);
        return mongo.updateMulti(query, update, BuyingGroup.class).getModifiedCount();
    }

    @Override
    public Page<BuyingGroup> search(GroupSearch search, Pageable pageable) {
        Criteria criteria = where(STATUS).is(search.status());
        if (search.cityKey() != null) {
            criteria = criteria.and("cityKey").is(search.cityKey());
        }
        if (search.brand() != null) {
            criteria = criteria.and("brand").is(search.brand());
        }
        if (search.modelKey() != null) {
            criteria = criteria.and("modelKey").is(search.modelKey());
        }

        Query query = new Query(criteria);
        long total = mongo.count(query, BuyingGroup.class);
        List<BuyingGroup> page = mongo.find(query.with(pageable), BuyingGroup.class);
        return new PageImpl<>(page, pageable, total);
    }

    private static FindAndModifyOptions returnNew() {
        return FindAndModifyOptions.options().returnNew(true);
    }
}
