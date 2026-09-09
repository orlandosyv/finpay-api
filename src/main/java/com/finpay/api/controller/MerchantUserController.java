package com.finpay.api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.finpay.api.dto.ApiError;
import com.finpay.api.dto.CreateMerchantUserRequest;
import com.finpay.api.dto.CurrentMerchantResponse;
import com.finpay.api.dto.MerchantUserResponse;
import com.finpay.api.service.MerchantUserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/merchant")
@Tag(name = "Merchant users", description = "View the current merchant and manage its users")
@SecurityRequirement(name = "bearerAuth")
@ApiResponses({
        @ApiResponse(
                responseCode = "401",
                description = "Authentication is required or the access token is invalid",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "403",
                description = "The authenticated role is not allowed to perform this operation",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
})
public class MerchantUserController {

    private final MerchantUserService merchantUserService;

    public MerchantUserController(MerchantUserService merchantUserService) {
        this.merchantUserService = merchantUserService;
    }

    @GetMapping("/me")
    @Operation(
            summary = "Get current merchant context",
            description = "Available to MERCHANT_ADMIN and MERCHANT_USER roles.")
    @ApiResponse(
            responseCode = "200",
            description = "Authenticated merchant context returned",
            content = @Content(schema = @Schema(implementation = CurrentMerchantResponse.class)))
    public CurrentMerchantResponse getCurrentMerchant() {
        return merchantUserService.getCurrentMerchant();
    }

    @GetMapping("/users")
    @Operation(
            summary = "List merchant users",
            description = "Available only to the MERCHANT_ADMIN role.")
    @ApiResponse(
            responseCode = "200",
            description = "Users from the authenticated merchant returned",
            content = @Content(array = @ArraySchema(
                    schema = @Schema(implementation = MerchantUserResponse.class))))
    public List<MerchantUserResponse> getMerchantUsers() {
        return merchantUserService.getMerchantUsers();
    }

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Create a merchant user",
            description = "Creates a user inside the authenticated merchant. Available only to MERCHANT_ADMIN.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Merchant user created",
                    content = @Content(schema = @Schema(implementation = MerchantUserResponse.class))),
            @ApiResponse(responseCode = "400", description = "Request validation failed"),
            @ApiResponse(responseCode = "409", description = "Email is already registered")
    })
    public MerchantUserResponse createMerchantUser(
            @Valid @RequestBody CreateMerchantUserRequest request) {
        return merchantUserService.createMerchantUser(request);
    }
}
