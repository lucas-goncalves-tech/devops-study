package com.notesapi.notes_api.auth;

import com.notesapi.notes_api.auth.dtos.*;
import com.notesapi.notes_api.auth.security.TokenService;
import com.notesapi.notes_api.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Auth")
public class AuthController {

    @Value("${spring.profiles.active}")
    private String activeProfile;

    private final UserService userService;
    private final TokenService tokenService;
    private final AuthService authService;


    private void setRefreshToCookies(HttpServletResponse response, String refreshToken, Long maxAgeInSeconds) {
        boolean isProd = "prod".equals(activeProfile);

        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                .secure(isProd)
                .httpOnly(true)
                .maxAge(maxAgeInSeconds)
                .sameSite("lax")
                .path("/")
                .build();

        response.setHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    @PostMapping("/register")
    @Operation(summary = "Cadastra uma novo usuário")
    ResponseEntity<RegisterResponse> create(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(userService.create(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Faz login com credenciais")
    ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                        HttpServletResponse response) {
        AuthTokens tokens = authService.login(request);
        setRefreshToCookies(response, tokens.refreshToken(), tokenService.getRefreshTokenExpirationInSeconds());

        return ResponseEntity.ok(new LoginResponse("Login realizado com sucesso!", tokens.accessToken()));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Gera uma novo access token e retora um novo refresh token se necessario")
    ResponseEntity<RefreshResponse> refresh(@CookieValue(name = "refreshToken", required = false) String refreshToken, HttpServletResponse response) {
        AuthTokens tokens = authService.refresh(refreshToken);

        if (!refreshToken.equals(tokens.refreshToken())) {
            setRefreshToCookies(response, tokens.refreshToken(), tokenService.getRefreshTokenExpirationInSeconds());
        }

        return ResponseEntity.ok(new RefreshResponse("Access token renovado com sucesso", tokens.accessToken()));
    }

    @PostMapping("/logout")
    @Operation(summary = "Realiza logout do usuário removendo o refresh token cookie")
    ResponseEntity<Void> logout(HttpServletResponse response) {
        setRefreshToCookies(response, "", 0L);
        return ResponseEntity.noContent().build();
    }
}
