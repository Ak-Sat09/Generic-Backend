package com.buytogether.group;

import com.buytogether.common.text.Text;

/**
 * Browse filters, already normalised. Being a record it is also a safe cache key.
 */
public record GroupSearch(String cityKey, CarBrand brand, String modelKey, GroupStatus status) {

    public static GroupSearch of(String city, CarBrand brand, String model, GroupStatus status) {
        return new GroupSearch(Text.key(city), brand, Text.key(model),
                status == null ? GroupStatus.OPEN : status);
    }
}
