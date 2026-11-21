package com.buytogether.group;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public enum GroupStatus {

    /** Accepting members. */
    OPEN,
    /** Target size reached; members are planning the showroom visit. */
    FULL,
    /** Members bought their cars. Read only, removed after the retention period. */
    COMPLETED,
    /** Cancelled by the creator. */
    CLOSED,
    /** Deadline passed. */
    EXPIRED;

    public static final Set<GroupStatus> ACTIVE = Collections.unmodifiableSet(EnumSet.of(OPEN, FULL));

    public static final Set<GroupStatus> FINISHED = Collections.unmodifiableSet(EnumSet.of(COMPLETED, CLOSED, EXPIRED));

    public boolean isActive() {
        return ACTIVE.contains(this);
    }
}
