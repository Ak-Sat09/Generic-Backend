package com.buytogether.group.cache;

import com.buytogether.common.error.BusinessException;
import com.buytogether.common.error.ErrorCode;
import com.buytogether.common.web.PageResponse;
import com.buytogether.group.CarBrand;
import com.buytogether.group.GroupSearch;
import com.buytogether.group.GroupStatus;
import com.buytogether.group.GroupView;
import com.buytogether.group.cache.GroupCache.SearchKey;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GuavaGroupCacheTest {

    private final GuavaGroupCache cache = new GuavaGroupCache(new CacheProperties(
            new CacheProperties.Spec(100, Duration.ofMinutes(10)),
            new CacheProperties.Spec(100, Duration.ofMinutes(1))));

    @Test
    void loadsGroupOnlyOnce() {
        AtomicInteger loads = new AtomicInteger();

        cache.group("g1", () -> { loads.incrementAndGet(); return view("g1"); });
        cache.group("g1", () -> { loads.incrementAndGet(); return view("g1"); });

        assertThat(loads).hasValue(1);
    }

    @Test
    void evictDropsTheGroupAndAllSearchPages() {
        AtomicInteger groupLoads = new AtomicInteger();
        AtomicInteger searchLoads = new AtomicInteger();
        SearchKey key = new SearchKey(GroupSearch.of("Jaipur", CarBrand.TATA, null, null), 0, 20);

        cache.group("g1", () -> { groupLoads.incrementAndGet(); return view("g1"); });
        cache.search(key, () -> { searchLoads.incrementAndGet(); return page(view("g1")); });

        cache.evict("g1");

        cache.group("g1", () -> { groupLoads.incrementAndGet(); return view("g1"); });
        cache.search(key, () -> { searchLoads.incrementAndGet(); return page(view("g1")); });
        assertThat(groupLoads).hasValue(2);
        assertThat(searchLoads).hasValue(2);
    }

    @Test
    void sameFiltersWrittenDifferentlyShareOneCacheEntry() {
        AtomicInteger loads = new AtomicInteger();

        cache.search(new SearchKey(GroupSearch.of("Jaipur", CarBrand.TATA, null, null), 0, 20),
                () -> { loads.incrementAndGet(); return page(); });
        cache.search(new SearchKey(GroupSearch.of("  jaipur ", CarBrand.TATA, null, GroupStatus.OPEN), 0, 20),
                () -> { loads.incrementAndGet(); return page(); });

        assertThat(loads).hasValue(1);
    }

    @Test
    void loaderErrorsAreRethrownAsIsAndNotCached() {
        assertThatThrownBy(() -> cache.group("missing", () -> {
            throw new BusinessException(ErrorCode.GROUP_NOT_FOUND);
        })).isInstanceOf(BusinessException.class);

        assertThat(cache.group("missing", () -> view("missing")).id()).isEqualTo("missing");
    }

    private static GroupView view(String id) {
        return new GroupView(id, CarBrand.TATA, "Nexon", "Jaipur", null, 10, 1, 9, GroupStatus.OPEN,
                "creator", null, Instant.parse("2026-11-21T10:00:00Z"));
    }

    private static PageResponse<GroupView> page(GroupView... views) {
        return new PageResponse<>(List.of(views), 0, 20, views.length, 1, true);
    }
}
