package com.buytogether.group;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface GroupRepository extends MongoRepository<BuyingGroup, String>, GroupRepositoryCustom {

    long countByCreatorIdAndStatusIn(String creatorId, Collection<GroupStatus> statuses);

    boolean existsByBrandAndCityKeyAndStatus(CarBrand brand, String cityKey, GroupStatus status);

    List<BuyingGroup> findByBrandAndCityKeyAndStatusOrderByMemberCountDescCreatedAtAsc(
            CarBrand brand, String cityKey, GroupStatus status, Pageable pageable);

    List<BuyingGroup> findByStatusInAndUpdatedAtBefore(Collection<GroupStatus> statuses, Instant before, Pageable pageable);
}
