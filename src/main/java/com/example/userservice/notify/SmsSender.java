package com.example.userservice.notify;

/**
 * Sends a text message. "log" prints it (development), "twilio" sends a real SMS.
 */
public interface SmsSender {

    void send(String e164Phone, String text);
}
