package com.notesapi.notes_api.note;

import com.notesapi.notes_api.exceptions.NotFoundException;
import com.notesapi.notes_api.note.dtos.*;
import com.notesapi.notes_api.note.entities.Note;
import com.notesapi.notes_api.user.entities.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NoteService {

    private final NoteRepository noteRepository;

    @Transactional(readOnly = true)
    public Page<NoteResponse> findAll(User user, String title, Pageable pageable) {
        Page<Note> notePage;
        String titleExists = title != null ? title.trim() : null;

        if (titleExists != null && !titleExists.isEmpty()) {
            notePage = noteRepository.findAllByUserAndTitleContainingIgnoreCase(user, titleExists, pageable);
        } else {
            notePage = noteRepository.findAllByUser(user, pageable);
        }

        return notePage.map(NoteResponse::fromEntity);
    }

    public CreateNoteResponse create(CreateNoteRequest data, User user) {
        Note newNote = Note.builder()
                .title(data.title())
                .content(data.content())
                .user(user)
                .build();

        Note createdNote = noteRepository.saveAndFlush(newNote);

        return new CreateNoteResponse("Nota %s criada com sucesso".formatted(data.title()),
                NoteResponse.fromEntity(createdNote));
    }

    @Transactional
    public UpdateNoteResponse update(UUID id, UpdateNoteRequest data, User user) {
        Note note = noteRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new NotFoundException("Nota não encontrada"));

        if (data.title() != null && !data.title().isEmpty()) {
            note.setTitle(data.title());
        }

        if (data.content() != null && !data.content().isEmpty()) {
            note.setContent(data.content());
        }

        Note updatedNote = noteRepository.save(note);

        return new UpdateNoteResponse("Nota %s atualizada com sucesso".formatted(updatedNote.getTitle()),
                NoteResponse.fromEntity(updatedNote));
    }

    @Transactional
    public ToggleCompletedResponse toggleCompleted(UUID id, User user) {
        Note note = noteRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new NotFoundException("Nota não encontrada"));

        note.setCompleted(!note.isCompleted());
        String completedMessage = note.isCompleted() ? "completada" : "não completada";

        Note updatedNote = noteRepository.save(note);

        return new ToggleCompletedResponse(
                "Nota %s atualizada para %s com sucesso".formatted(updatedNote.getTitle(), completedMessage),
                NoteResponse.fromEntity(updatedNote));
    }

    @Transactional
    public void delete(UUID id, User user) {
        Note note = noteRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new NotFoundException("Nota não encontrada"));

        noteRepository.delete(note);
    }

}
