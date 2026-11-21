package com.buytogether.group;

import java.util.List;

/**
 * Answer to "anyone already buying this brand in my city?", shown before the create form.
 * When {@code canCreate} is false the user should join one of {@code openGroups} instead.
 */
public record GroupSuggestions(
        CarBrand brand,
        String brandName,
        String city,
        boolean canCreate,
        int peopleInterested,
        List<GroupResponse> openGroups
) {
}
