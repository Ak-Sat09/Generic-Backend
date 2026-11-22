package com.buytogether.common.error;

import org.springframework.http.ProblemDetail;

import java.util.Map;

/**
 * Builds RFC 7807 error bodies with an extra machine readable {@code code} field,
 * so the frontend can switch on the code instead of parsing messages.
 */
public final class ProblemDetails {

    private ProblemDetails() {
    }

    public static ProblemDetail of(ErrorCode code, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(code.status(), detail);
        problem.setTitle(code.status().getReasonPhrase());
        problem.setProperty("code", code.name());
        return problem;
    }

    public static ProblemDetail of(ErrorCode code, String detail, Map<String, String> fieldErrors) {
        ProblemDetail problem = of(code, detail);
        problem.setProperty("fieldErrors", fieldErrors);
        return problem;
    }
}
