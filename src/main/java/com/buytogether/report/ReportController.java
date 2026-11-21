package com.buytogether.report;

import com.buytogether.security.AuthUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @PostMapping("/api/groups/{groupId}/reports")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> report(@PathVariable String groupId,
                                      @AuthenticationPrincipal AuthUser user,
                                      @Valid @RequestBody ReportRequest request) {
        String id = reportService.report(groupId, user.id(), request.reason(), request.details());
        return Map.of("id", id);
    }
}
