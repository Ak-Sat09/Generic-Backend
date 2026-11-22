package com.buytogether.user;

import com.buytogether.common.error.BusinessException;
import com.buytogether.common.error.ErrorCode;
import com.buytogether.common.phone.PhoneNumber;
import com.buytogether.common.text.Text;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserAccountRepository users;
    private final Clock clock;

    /**
     * Phone login doubles as sign up: the first verified login creates the account.
     */
    public SignInResult signIn(PhoneNumber phone, String nameOnSignUp) {
        return users.findByPhone(phone.value())
                .map(existing -> new SignInResult(existing, false))
                .orElseGet(() -> register(phone, nameOnSignUp));
    }

    public User get(String userId) {
        return users.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    public User updateProfile(String userId, String name, String city) {
        User user = get(userId);
        user.setName(Text.clean(name));
        user.setCity(Text.clean(city));
        user.setUpdatedAt(clock.instant());
        return users.save(user);
    }

    public Map<String, User> findAllById(Collection<String> ids) {
        return users.findAllById(ids).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private SignInResult register(PhoneNumber phone, String name) {
        Instant now = clock.instant();
        User user = User.builder()
                .phone(phone.value())
                .name(Text.clean(name))
                .createdAt(now)
                .updatedAt(now)
                .build();
        try {
            return new SignInResult(users.save(user), true);
        } catch (DuplicateKeyException raceWithAnotherLogin) {
            User existing = users.findByPhone(phone.value()).orElseThrow(() -> raceWithAnotherLogin);
            return new SignInResult(existing, false);
        }
    }

    public record SignInResult(User user, boolean newUser) {
    }
}
