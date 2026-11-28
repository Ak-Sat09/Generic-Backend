package com.example.userservice.notify;

import com.example.userservice.common.UserServiceException;
import com.example.userservice.config.UserServiceProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;

/**
 * Twilio's Messages API over plain HTTP, no SDK.
 */
@Component
@ConditionalOnProperty(prefix = "user-service.sms", name = "sender", havingValue = "twilio")
class TwilioSmsSender implements SmsSender {

    private static final Logger log = LoggerFactory.getLogger(TwilioSmsSender.class);

    private final RestClient client;
    private final String accountSid;
    private final String fromNumber;

    TwilioSmsSender(UserServiceProperties properties) {
        UserServiceProperties.Sms sms = properties.sms();
        if (!StringUtils.hasText(sms.twilioAccountSid()) || !StringUtils.hasText(sms.twilioAuthToken())
                || !StringUtils.hasText(sms.twilioFromNumber())) {
            throw new IllegalStateException("Twilio account SID, auth token and from number must be set when the SMS sender is twilio");
        }
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(10));
        this.client = RestClient.builder()
                .baseUrl("https://api.twilio.com/2010-04-01")
                .requestFactory(factory)
                .defaultHeaders(headers -> headers.setBasicAuth(sms.twilioAccountSid(), sms.twilioAuthToken()))
                .build();
        this.accountSid = sms.twilioAccountSid();
        this.fromNumber = sms.twilioFromNumber();
    }

    @Override
    public void send(String e164Phone, String text) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("To", e164Phone);
        form.add("From", fromNumber);
        form.add("Body", text);
        try {
            client.post().uri("/Accounts/{sid}/Messages.json", accountSid)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.warn("Twilio could not send SMS: {}", e.getMessage());
            throw new UserServiceException(HttpStatus.SERVICE_UNAVAILABLE, "Could not send the SMS right now, try again");
        }
    }
}
