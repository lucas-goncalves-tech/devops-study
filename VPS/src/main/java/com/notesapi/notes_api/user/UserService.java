package com.notesapi.notes_api.user;

import com.notesapi.notes_api.auth.dtos.RegisterRequest;
import com.notesapi.notes_api.auth.dtos.RegisterResponse;
import com.notesapi.notes_api.exceptions.ConflictException;
import com.notesapi.notes_api.user.dto.UserResponse;
import com.notesapi.notes_api.user.entities.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public RegisterResponse create(RegisterRequest data) {
        if (userRepository.existsByEmail(data.email())) {
            throw new ConflictException("Email já existe");
        }

        String encodedPassword = passwordEncoder.encode(data.password());

        User newUser = User.builder()
                .email(data.email())
                .displayName(data.displayName())
                .password(encodedPassword)
                .build();
        User createdUser = userRepository.save(newUser);

        return new RegisterResponse("Usuário %s criado com sucesso".formatted(data.displayName()),
                UserResponse.fromEntity(createdUser));
    }

}
