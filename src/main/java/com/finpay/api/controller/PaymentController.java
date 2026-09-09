package com.finpay.api.controller;

import com.finpay.api.dto.CreatePaymentRequest;
import com.finpay.api.dto.ApiError;
import com.finpay.api.dto.PaymentResponse;
import com.finpay.api.mapper.PaymentMapper;
import com.finpay.api.service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/payments")
@Tag(name = "Payments", description = "Create payments and manage their lifecycle")
@SecurityRequirement(name = "bearerAuth")
@ApiResponse(
        responseCode = "401",
        description = "Authentication is required or the access token is invalid",
        content = @Content(schema = @Schema(implementation = ApiError.class)))
@ApiResponse(
        responseCode = "403",
        description = "The authenticated role is not allowed to perform this operation",
        content = @Content(schema = @Schema(implementation = ApiError.class)))
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentMapper paymentMapper;

    public PaymentController(
            PaymentService paymentService,
            PaymentMapper paymentMapper) {
        this.paymentService = paymentService;
        this.paymentMapper = paymentMapper;
    }

    @GetMapping
    @Operation(
            summary = "List payments",
            description = "Returns payments owned by the authenticated merchant. Available to both merchant roles.")
    @ApiResponse(
            responseCode = "200",
            description = "Payments returned successfully",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = PaymentResponse.class))))
    public List<PaymentResponse> getPayments() {
        return paymentService.getAllPayments()
                .stream()
                .map(paymentMapper::toResponse)
                .toList();
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Find a payment",
            description = "Returns one payment owned by the authenticated merchant. Available to both merchant roles.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment found"),
            @ApiResponse(responseCode = "404", description = "Payment does not exist")
    })
    public PaymentResponse getPaymentById(
            @Parameter(description = "Payment identifier", example = "1")
            @PathVariable Long id) {
        return paymentMapper.toResponse(paymentService.getPaymentById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Create a payment",
            description = "Creates a PENDING payment for the authenticated merchant. Available to both merchant roles.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Payment created"),
            @ApiResponse(responseCode = "400", description = "Request validation failed")
    })
    public PaymentResponse createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        return paymentMapper.toResponse(paymentService.createPayment(request));
    }

    @PatchMapping("/{id}/approve")
    @Operation(
            summary = "Approve a payment",
            description = "Transitions a PENDING payment to APPROVED. Available only to MERCHANT_ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment approved"),
            @ApiResponse(responseCode = "404", description = "Payment does not exist"),
            @ApiResponse(responseCode = "409", description = "Invalid status transition")
    })
    public PaymentResponse approvePayment(
            @Parameter(description = "Payment identifier", example = "1")
            @PathVariable Long id) {
        return paymentMapper.toResponse(paymentService.approvePayment(id));
    }

    @PatchMapping("/{id}/decline")
    @Operation(
            summary = "Decline a payment",
            description = "Transitions a PENDING payment to DECLINED. Available only to MERCHANT_ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment declined"),
            @ApiResponse(responseCode = "404", description = "Payment does not exist"),
            @ApiResponse(responseCode = "409", description = "Invalid status transition")
    })
    public PaymentResponse declinePayment(
            @Parameter(description = "Payment identifier", example = "1")
            @PathVariable Long id) {
        return paymentMapper.toResponse(paymentService.declinePayment(id));
    }

    @PatchMapping("/{id}/refund")
    @Operation(
            summary = "Refund a payment",
            description = "Transitions an APPROVED payment to REFUNDED. Available only to MERCHANT_ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment refunded"),
            @ApiResponse(responseCode = "404", description = "Payment does not exist"),
            @ApiResponse(responseCode = "409", description = "Invalid status transition")
    })
    public PaymentResponse refundPayment(
            @Parameter(description = "Payment identifier", example = "1")
            @PathVariable Long id) {
        return paymentMapper.toResponse(paymentService.refundPayment(id));
    }

}
