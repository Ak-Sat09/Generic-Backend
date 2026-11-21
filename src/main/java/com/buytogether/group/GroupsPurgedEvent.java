package com.buytogether.group;

import java.util.List;

/**
 * Published just before finished groups are deleted, so other modules (chat, reports)
 * can remove their own data without the group module depending on them.
 */
public record GroupsPurgedEvent(List<String> groupIds) {
}
