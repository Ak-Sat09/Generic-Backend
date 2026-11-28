package com.example.userservice.account;

import java.time.Instant;
import java.util.Optional;

interface AccountTokenStore {

    void save(AccountToken token);

    /** Marks the token used if it is unused, unexpired and for this purpose, and returns it. */
    Optional<AccountToken> consume(String tokenHash, AccountToken.Purpose purpose, Instant now);

    /** Older links of the same kind stop working when a new one is sent or one is used. */
    void retireAll(Long userId, AccountToken.Purpose purpose, Instant now);
}
