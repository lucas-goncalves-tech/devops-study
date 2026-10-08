package com.notesapi.notes_api.auth;

import com.notesapi.notes_api.auth.dtos.AuthTokens;
import com.notesapi.notes_api.auth.dtos.LoginRequest;
import com.notesapi.notes_api.auth.security.TokenService;
import com.notesapi.notes_api.exceptions.UnauthorizedException;
import com.notesapi.notes_api.user.UserRepository;
import com.notesapi.notes_api.user.entities.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private TokenService tokenService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthService authService;

    @Nested
    @DisplayName("Method: login()")
    class LoginMethod {

        @Test
        @DisplayName("Should generate access and refresh tokens when credentials is valid")
        void shouldGenerateAccessAndRefreshTokens_WhenCredentialsIsValid() {
            String validEmail = "test@test.com";
            String validPassword = "123123123";
            String fakeAccessToken = "fake-access-token";
            String fakeRefreshToken = "fake-refresh-token";
            LoginRequest request = new LoginRequest(validEmail, validPassword);
            User mockUser = User.builder().email(validEmail).build();
            Authentication authResponse = new UsernamePasswordAuthenticationToken(mockUser, null);

            when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authResponse);
            when(tokenService.generateAccess(mockUser)).thenReturn(fakeAccessToken);
            when(tokenService.generateRefresh(mockUser)).thenReturn(fakeRefreshToken);

            AuthTokens response = authService.login(request);

            assertNotNull(response);
            assertEquals(fakeAccessToken, response.accessToken());
            assertEquals(fakeRefreshToken, response.refreshToken());

            var captor = ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
            verify(authenticationManager).authenticate(captor.capture());

            var capturedToken = captor.getValue();
            assertEquals(validEmail, capturedToken.getPrincipal());
            assertEquals(validPassword, capturedToken.getCredentials());
        }

        @Test
        @DisplayName("Should throw BadCredentialsExeception when credentials is invalid")
        void shouldThrowBadCredentialsExeception_WhenCredentialsIsInvalid() {
            LoginRequest request = new LoginRequest("test@test.com", "123123123");

            when(authenticationManager.authenticate(any(Authentication.class)))
                    .thenThrow(new BadCredentialsException("invalid credentials"));

            assertThrows(BadCredentialsException.class, () -> authService.login(request));

            verify(tokenService, never()).generateAccess(any(User.class));
            verify(tokenService, never()).generateRefresh(any(User.class));
        }
    }

    @Nested
    @DisplayName("Method: refresh()")
    class RefreshMethod {

        @Test
        @DisplayName("Should generate new access and refresh token when refreshToken is valid")
        void shouldGenerateNewAccessAndRefreshToken_WhenRefreshTokenIsValid() {
            String validRefreshToken = "valid-refresh-token";
            String fakeAccessToken = "fake-access-token";
            String fakeEmail = "test@test.com";
            User fakeUser = User.builder().email(fakeEmail).build();

            when(tokenService.validate(validRefreshToken, TokenService.TokenType.REFRESH)).thenReturn(fakeEmail);
            when(userRepository.findByEmail(fakeEmail)).thenReturn(Optional.of(fakeUser));
            when(tokenService.generateAccess(fakeUser)).thenReturn(fakeAccessToken);

            AuthTokens response = authService.refresh(validRefreshToken);

            assertNotNull(response);
            assertEquals(fakeAccessToken, response.accessToken());
            assertEquals(validRefreshToken, response.refreshToken());
        }

        @Test
        @DisplayName("Should generate new access and new refresh token when refreshToken ttl is half")
        void shouldGenerateNewAccessAndNewRefreshToken_WhenRefreshTokenTtlIsHalf() {
            String validRefreshToken = "valid-refresh-token";
            String fakeAccessToken = "fake-access-token";
            String fakeRefreshToken = "fake-refresh-token";
            String fakeEmail = "test@test.com";
            User fakeUser = User.builder().email(fakeEmail).build();

            when(tokenService.validate(validRefreshToken, TokenService.TokenType.REFRESH)).thenReturn(fakeEmail);
            when(userRepository.findByEmail(fakeEmail)).thenReturn(Optional.of(fakeUser));
            when(tokenService.needsNewRefresh(validRefreshToken)).thenReturn(true);
            when(tokenService.generateAccess(fakeUser)).thenReturn(fakeAccessToken);
            when(tokenService.generateRefresh(fakeUser)).thenReturn(fakeRefreshToken);

            AuthTokens response = authService.refresh(validRefreshToken);

            assertNotNull(response);
            assertEquals(fakeAccessToken, response.accessToken());
            assertEquals(fakeRefreshToken, response.refreshToken());
        }

        @Test
        @DisplayName("Should throw UnauthorizedExpection when refresh token is empty")
        void shouldThrowUnauthorizedException_WhenRefreshTokenIsEmpty() {
            assertThrows(UnauthorizedException.class, () -> authService.refresh(""));

            verify(tokenService, never()).validate(anyString(), any(TokenService.TokenType.class));
            verify(userRepository, never()).findByEmail(anyString());
            verify(tokenService, never()).generateAccess(any(User.class));
            verify(tokenService, never()).needsNewRefresh(anyString());
        }

        @Test
        @DisplayName("Should throw UnauthorizedException when refresh token is invalid")
        void shouldThrowUnauthorizedException_WhenRefreshTokenIsInvalid() {
            String invalidRefreshToken = "invalid-refresh-token";

            when(tokenService.validate(invalidRefreshToken, TokenService.TokenType.REFRESH)).thenReturn(null);

            assertThrows(UnauthorizedException.class, () -> authService.refresh(invalidRefreshToken));

            verify(userRepository, never()).findByEmail(anyString());
            verify(tokenService, never()).generateAccess(any(User.class));
            verify(tokenService, never()).needsNewRefresh(anyString());
        }

        @Test
        @DisplayName("Should throw UnauthorizedException when user not found")
        void shouldThrowUnauthorizedException_WhenUserNotFound() {
            String validRefreshToken = "valid-refresh-token";
            String invalidEmail = "test@test.com";

            when(tokenService.validate(validRefreshToken, TokenService.TokenType.REFRESH)).thenReturn(invalidEmail);
            when(userRepository.findByEmail(invalidEmail)).thenReturn(Optional.empty());

            assertThrows(UnauthorizedException.class, () -> authService.refresh(validRefreshToken));

            verify(tokenService, never()).generateAccess(any(User.class));
            verify(tokenService, never()).needsNewRefresh(anyString());
        }
    }
}
