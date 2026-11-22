package com.buytogether.group.cache;

import com.buytogether.common.web.PageResponse;
import com.buytogether.group.GroupSearch;
import com.buytogether.group.GroupView;

import java.util.function.Supplier;

/**
 * Read cache for groups. Services depend on this interface only, so the Guava
 * implementation can be replaced (Caffeine, Redis) without touching business code.
 *
 * Only data that is the same for every viewer is cached. Per user flags such as
 * "joined" are added after the cache lookup.
 */
public interface GroupCache {

    GroupView group(String groupId, Supplier<GroupView> loader);

    PageResponse<GroupView> search(SearchKey key, Supplier<PageResponse<GroupView>> loader);

    /** Call after any change to a group: drops that group and every cached search page. */
    void evict(String groupId);

    /** Call after a change that touches many groups at once. */
    void evictAll();

    record SearchKey(GroupSearch search, int page, int size) {
    }
}
