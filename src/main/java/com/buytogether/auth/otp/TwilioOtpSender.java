package com.buytogether.auth.otp;

import com.buytogether.common.phone.PhoneNumber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
 * Sends the code as an SMS through Twilio's Messages API.
 * Plain HTTP call, so no Twilio SDK is needed.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "buy-together.otp", name = "sender", havingValue = "twilio")
class TwilioOtpSender implements OtpSender {

    static final String BASE_URL = "https://api.twilio.com/2010-04-01";

    private final RestClient client;
    private final String accountSid;
    private final String fromNumber;
    private final long ttlMinutes;

    @Autowired
    TwilioOtpSender(TwilioProperties twilio, OtpProperties otp) {
        this(twilio, otp, RestClient.builder().requestFactory(timeouts()));
    }

    TwilioOtpSender(TwilioProperties twilio, OtpProperties otp, RestClient.Builder builder) {
        requireSet(twilio.accountSid(), "account-sid");
        requireSet(twilio.authToken(), "auth-token");
        requireSet(twilio.fromNumber(), "from-number");

        this.client = builder
                .baseUrl(BASE_URL)
                .defaultHeaders(headers -> headers.setBasicAuth(twilio.accountSid(), twilio.authToken()))
                .build();
        this.accountSid = twilio.accountSid();
        this.fromNumber = twilio.fromNumber();
        this.ttlMinutes = Math.max(1, otp.ttl().toMinutes());
    }

    @Override
    public void send(PhoneNumber phone, String code) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("To", phone.value());
        form.add("From", fromNumber);
        form.add("Body", code + " is your Buy Together login code. It expires in " + ttlMinutes + " minutes.");

        try {
            client.post()
                    .uri("/Accounts/{sid}/Messages.json", accountSid)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toBodilessEntity();
            log.info("OTP SMS sent to {}", phone.masked());
        } catch (RestClientException e) {
            log.warn("Twilio could not send OTP to {}: {}", phone.masked(), e.getMessage());
            throw new OtpDeliveryException(e);
        }
    }

    private static SimpleClientHttpRequestFactory timeouts() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(10));
        return factory;
    }

    private static void requireSet(String value, String key) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException("buy-together.otp.twilio." + key + " must be set when the OTP sender is twilio");
        }
    }
}
