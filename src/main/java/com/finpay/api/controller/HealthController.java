package com.finpay.api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "Health", description = "API availability")
public class HealthController {

    @GetMapping("/api/health")
    @Operation(summary = "Check API health", description = "Confirms that the FinPay API is running.")
    @ApiResponse(responseCode = "200", description = "API is available")
    public String health() {
        return "FinPay API is running";
    }
}
