package com.example.userservice.notify;

/**
 * Account emails. "log" prints them (development), "smtp" sends them through spring.mail.
 */
public interface AccountMailer {

    void sendVerificationLink(String to, String name, String link);

    void sendPasswordResetLink(String to, String link);

    void sendLoginCode(String to, String code, long validMinutes);
}
