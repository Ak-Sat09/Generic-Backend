package com.buytogether.auth.otp;

import com.buytogether.common.phone.PhoneNumber;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class TwilioOtpSenderTest {

    private static final PhoneNumber PHONE = PhoneNumber.parse("9876543210");
    private static final String MESSAGES_URL = TwilioOtpSender.BASE_URL + "/Accounts/AC123/Messages.json";

    private final OtpProperties otp = new OtpProperties(6, Duration.ofMinutes(5), Duration.ofSeconds(60), 3, "twilio");
    private MockRestServiceServer server;
    private TwilioOtpSender sender;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        sender = new TwilioOtpSender(new TwilioProperties("AC123", "secret", "+15005550006"), otp, builder);
    }

    @Test
    void postsTheCodeToTwilio() {
        server.expect(requestTo(MESSAGES_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Basic QUMxMjM6c2VjcmV0"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(content().string(containsString("To=%2B919876543210")))
                .andExpect(content().string(containsString("Body=123456+is+your+Buy+Together+login+code")))
                .andRespond(withSuccess("{\"sid\":\"SM1\"}", MediaType.APPLICATION_JSON));

        sender.send(PHONE, "123456");

        server.verify();
    }

    @Test
    void gatewayErrorBecomesDeliveryFailure() {
        server.expect(requestTo(MESSAGES_URL)).andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertThatThrownBy(() -> sender.send(PHONE, "123456")).isInstanceOf(OtpDeliveryException.class);
    }

    @Test
    void refusesToStartWithoutCredentials() {
        assertThatThrownBy(() -> new TwilioOtpSender(new TwilioProperties("AC123", "", "+15005550006"), otp, RestClient.builder()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("auth-token");
    }
}
