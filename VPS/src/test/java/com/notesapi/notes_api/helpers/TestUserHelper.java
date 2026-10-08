package com.notesapi.notes_api.helpers;

import com.notesapi.notes_api.user.UserRepository;
import com.notesapi.notes_api.user.entities.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TestUserHelper {
    private String email = "test@test.com";
    private String displayName = "Default User";
    private String rawPassword = "123123123";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public TestUserHelper email(String email) {
        this.email = email;
        return this;
    }

    public TestUserHelper displayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

    public TestUserHelper rawPassword(String rawPassword) {
        this.rawPassword = rawPassword;
        return this;
    }

    public User createUser() {
        User user = User.builder()
                .email(email)
                .displayName(displayName)
                .password(passwordEncoder.encode(rawPassword))
                .build();
        return userRepository.save(user);
    }


}
