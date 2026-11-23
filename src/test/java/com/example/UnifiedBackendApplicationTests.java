package com.example;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Starts the whole application (no database needed: MongoDB connects lazily) and checks
 * that the Buy Together module lives under /buy-together without changing the other modules.
 */
@SpringBootTest(properties = "spring.data.mongodb.auto-index-creation=false")
@AutoConfigureMockMvc
class UnifiedBackendApplicationTests {

    @Autowired
    private MockMvc mvc;

    @Test
    void existingEndpointsAreNotAffectedByBuyTogetherSecurity() throws Exception {
        mvc.perform(get("/api/hello"))
                .andExpect(status().isOk())
                .andExpect(content().string("Hello World"));
    }

    @Test
    void buyTogetherEndpointsAreServedUnderTheirPrefix() throws Exception {
        mvc.perform(get("/buy-together/api/brands"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").exists());

        // Without the prefix the module must not answer (the exact error code comes from the host app)
        mvc.perform(get("/api/brands"))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotEqualTo(200));
    }

    @Test
    void buyTogetherProtectedEndpointsNeedLogin() throws Exception {
        mvc.perform(post("/buy-together/api/groups").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void buyTogetherErrorsKeepTheirOwnFormat() throws Exception {
        mvc.perform(post("/buy-together/api/auth/otp/send").contentType(MediaType.APPLICATION_JSON).content("{\"phone\":\"123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PHONE"));
    }

    @Test
    void buyTogetherEmailLoginIsReachable() throws Exception {
        mvc.perform(post("/buy-together/api/auth/email/send").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_EMAIL"));
    }

    @Test
    void buyTogetherGoogleLoginIsOffWithoutClientId() throws Exception {
        mvc.perform(post("/buy-together/api/auth/google").contentType(MediaType.APPLICATION_JSON).content("{\"credential\":\"x\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("GOOGLE_LOGIN_DISABLED"));
    }
}
