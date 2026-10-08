package com.notesapi.notes_api.note;


import com.notesapi.notes_api.note.entities.Note;
import com.notesapi.notes_api.user.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface NoteRepository extends JpaRepository<Note, UUID> {
    Page<Note> findAllByUser(User user, Pageable pageable);

    Page<Note> findAllByUserAndTitleContainingIgnoreCase(User user, String title, Pageable pageable);

    Optional<Note> findByIdAndUser(UUID id, User user);
}
