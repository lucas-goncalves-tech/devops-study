package com.notesapi.notes_api.user;

import com.notesapi.notes_api.auth.dtos.RegisterRequest;
import com.notesapi.notes_api.auth.dtos.RegisterResponse;
import com.notesapi.notes_api.exceptions.ConflictException;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Nested
    @DisplayName("Method: create()")
    class CreateMethod {
        @Test
        @DisplayName("Should throw ConflictException when email already exists")
        void shouldThrowConflictException_WhenEmailAlreadyExists() {
            String existEmail = "test@test.com";
            RegisterRequest registerRequest = new RegisterRequest(existEmail, "username", "password123", "password123");

            when(userRepository.existsByEmail(registerRequest.email())).thenReturn(true);

            assertThrows(ConflictException.class, () -> userService.create(registerRequest));

            verify(passwordEncoder, never()).encode(anyString());
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Should create a new user when email nonexists")
        void shouldCreateNewUser_WhenEmailNonExists() {
            String newEmail = "test@test.com";
            String password = "password123123";
            String encodedPass = "encoded_pass";
            RegisterRequest request = new RegisterRequest(newEmail, "username", password, password);
            User newUser = User.builder()
                    .email(request.email())
                    .displayName(request.displayName())
                    .password(encodedPass)
                    .build();

            when(userRepository.existsByEmail(request.email())).thenReturn(false);
            when(passwordEncoder.encode(request.password())).thenReturn(encodedPass);
            when(userRepository.save(any(User.class))).thenReturn(newUser);

            RegisterResponse response = userService.create(request);

            assertNotNull(response);
            assertNotNull(response.message());
            assertFalse(response.message().isBlank());
            assertEquals(newEmail, response.data().email());

            verify(passwordEncoder).encode(request.password());
            verify(userRepository).existsByEmail(request.email());
            verify(userRepository).save(any(User.class));

            var captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());

            var capturedSaveduser = captor.getValue();
            assertEquals(encodedPass, capturedSaveduser.getPassword());
        }
    }


}
