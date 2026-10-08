package com.notesapi.notes_api.user;

import org.springframework.data.jpa.repository.JpaRepository;

import com.notesapi.notes_api.user.entities.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
