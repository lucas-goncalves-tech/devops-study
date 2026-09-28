package com.securepay.ledger.payment;

import com.securepay.ledger.payment.dto.TransactionResponse;
import com.securepay.ledger.payment.dto.TransferRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/transfer")
    public ResponseEntity<TransactionResponse> transfer(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody TransferRequest request
    ) {
        TransactionResponse response = paymentService.transfer(
                userDetails.getUsername(),
                request,
                idempotencyKey
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponse> getTransaction(@PathVariable UUID id) {
        TransactionResponse response = paymentService.getTransaction(id);
        return ResponseEntity.ok(response);
    }
}
