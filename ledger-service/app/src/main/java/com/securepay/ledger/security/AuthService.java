package com.securepay.ledger.security;

import com.securepay.ledger.domain.entity.Account;
import com.securepay.ledger.domain.entity.AccountRole;
import com.securepay.ledger.domain.entity.Wallet;
import com.securepay.ledger.domain.repository.AccountRepository;
import com.securepay.ledger.domain.repository.WalletRepository;
import com.securepay.ledger.security.dto.AuthResponse;
import com.securepay.ledger.security.dto.LoginRequest;
import com.securepay.ledger.security.dto.RegisterRequest;
import com.securepay.ledger.security.dto.RegisterResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

@Service
public class AuthService {

    private final AccountRepository accountRepository;
    private final WalletRepository walletRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    public AuthService(
            AccountRepository accountRepository,
            WalletRepository walletRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthenticationManager authenticationManager,
            UserDetailsService userDetailsService
    ) {
        this.accountRepository = accountRepository;
        this.walletRepository = walletRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (accountRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email já cadastrado no sistema: " + request.email());
        }

        Account account = Account.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .fullName(request.fullName())
                .role(AccountRole.ROLE_USER)
                .createdAt(Instant.now())
                .build();

        Account savedAccount = accountRepository.save(account);

        Wallet wallet = Wallet.builder()
                .account(savedAccount)
                .balance(BigDecimal.ZERO)
                .currency("BRL")
                .updatedAt(Instant.now())
                .build();

        Wallet savedWallet = walletRepository.save(wallet);

        return new RegisterResponse(
                savedAccount.getId(),
                savedAccount.getEmail(),
                savedAccount.getFullName(),
                savedWallet.getId()
        );
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.email());
        String token = jwtService.generateToken(userDetails);

        return new AuthResponse(token, jwtService.getJwtExpiration() / 1000, "Bearer");
    }
}
