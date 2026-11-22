package com.buytogether.group.cache;

import com.buytogether.common.web.PageResponse;
import com.buytogether.group.GroupView;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.UncheckedExecutionException;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

@Component
class GuavaGroupCache implements GroupCache {

    private final Cache<String, GroupView> groups;
    private final Cache<SearchKey, PageResponse<GroupView>> searches;

    GuavaGroupCache(CacheProperties properties) {
        this.groups = build(properties.group());
        this.searches = build(properties.search());
    }

    @Override
    public GroupView group(String groupId, Supplier<GroupView> loader) {
        return load(groups, groupId, loader);
    }

    @Override
    public PageResponse<GroupView> search(SearchKey key, Supplier<PageResponse<GroupView>> loader) {
        return load(searches, key, loader);
    }

    @Override
    public void evict(String groupId) {
        groups.invalidate(groupId);
        // Search pages are short lived and any change can move a group between pages,
        // so dropping all of them is simpler and safer than working out which ones changed.
        searches.invalidateAll();
    }

    @Override
    public void evictAll() {
        groups.invalidateAll();
        searches.invalidateAll();
    }

    private static <K, V> Cache<K, V> build(CacheProperties.Spec spec) {
        return CacheBuilder.newBuilder()
                .maximumSize(spec.maxSize())
                .expireAfterWrite(spec.ttl())
                .build();
    }

    /**
     * Guava wraps whatever the loader throws. Unwrap it so a "group not found"
     * from the loader reaches the client as a 404 and not as a 500.
     */
    private static <K, V> V load(Cache<K, V> cache, K key, Supplier<V> loader) {
        try {
            return cache.get(key, loader::get);
        } catch (ExecutionException | UncheckedExecutionException e) {
            if (e.getCause() instanceof RuntimeException cause) {
                throw cause;
            }
            throw new IllegalStateException("Cache loader failed", e.getCause());
        }
    }
}
