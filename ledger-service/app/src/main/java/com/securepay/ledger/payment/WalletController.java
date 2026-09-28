package com.securepay.ledger.payment;

import com.securepay.ledger.payment.dto.DepositRequest;
import com.securepay.ledger.payment.dto.WalletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/wallets")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping("/me")
    public ResponseEntity<WalletResponse> getMyWallet(@AuthenticationPrincipal UserDetails userDetails) {
        WalletResponse response = walletService.getWalletByEmail(userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/deposit")
    public ResponseEntity<WalletResponse> deposit(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody DepositRequest request
    ) {
        WalletResponse response = walletService.deposit(
                userDetails.getUsername(),
                request.amount(),
                request.currency()
        );
        return ResponseEntity.ok(response);
    }
}
