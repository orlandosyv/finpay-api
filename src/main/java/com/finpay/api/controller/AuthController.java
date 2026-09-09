package com.finpay.api.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.finpay.api.dto.RegisterMerchantRequest;
import com.finpay.api.dto.RegistrationResponse;
import com.finpay.api.dto.LoginRequest;
import com.finpay.api.dto.LoginResponse;
import com.finpay.api.service.AuthenticationService;
import com.finpay.api.service.RegistrationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Merchant account registration and authentication")
public class AuthController {

    private final RegistrationService registrationService;
    private final AuthenticationService authenticationService;

    public AuthController(
            RegistrationService registrationService,
            AuthenticationService authenticationService) {
        this.registrationService = registrationService;
        this.authenticationService = authenticationService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Register a merchant",
            description = "Creates an active merchant, its first active user, and an administrator membership.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Merchant registered",
                    content = @Content(schema = @Schema(implementation = RegistrationResponse.class))),
            @ApiResponse(responseCode = "400", description = "Request validation failed"),
            @ApiResponse(responseCode = "409", description = "Email is already registered")
    })
    public RegistrationResponse register(
            @Valid @RequestBody RegisterMerchantRequest request) {
        return registrationService.register(request);
    }

    @PostMapping("/login")
    @Operation(
            summary = "Log in",
            description = "Validates merchant credentials and returns a signed JWT access token.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Authentication successful",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "400", description = "Request validation failed"),
            @ApiResponse(responseCode = "401", description = "Invalid email or password")
    })
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authenticationService.login(request);
    }
}
