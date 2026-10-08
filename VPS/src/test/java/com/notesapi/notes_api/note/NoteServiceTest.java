package com.notesapi.notes_api.note;

import com.notesapi.notes_api.exceptions.NotFoundException;
import com.notesapi.notes_api.note.dtos.*;
import com.notesapi.notes_api.note.entities.Note;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
public class NoteServiceTest {

    @Mock
    NoteRepository noteRepository;

    @InjectMocks
    NoteService noteService;

    @Nested
    @DisplayName("Method: findAll()")
    class findAllMethod {
        @Test
        @DisplayName("Should return all notes from user")
        void shouldReturnAllNotesFromUser() {
            User mockUser = User.builder().email("test@test").build();
            Note mockNote = Note.builder().title("title").build();
            Pageable pageable = PageRequest.of(0, 10);
            Page<Note> notePage = new PageImpl<>(List.of(mockNote));

            when(noteRepository.findAllByUser(any(User.class), any(Pageable.class))).thenReturn(notePage);

            Page<NoteResponse> response = noteService.findAll(mockUser, null, pageable);

            assertNotNull(response);
            assertEquals(1, response.getTotalElements());
            assertEquals(1, response.getTotalPages());

            verify(noteRepository).findAllByUser(mockUser, pageable);
            verify(noteRepository, never()).findAllByUserAndTitleContainingIgnoreCase(any(User.class), anyString(), any(Pageable.class));
        }

        @Test
        @DisplayName("Should return all notes from user when title is notEmpty")
        void shouldReturnAllNotesFromUser_WhenTitleIsNotEmpty() {
            String title = "searched title";
            User mockUser = User.builder().email("test@test").build();
            Note mockNote = Note.builder().title(title).build();
            Pageable pageable = PageRequest.of(0, 10);
            Page<Note> notePage = new PageImpl<>(List.of(mockNote));

            when(noteRepository.findAllByUserAndTitleContainingIgnoreCase(any(User.class), anyString(), any(Pageable.class))).thenReturn(notePage);

            Page<NoteResponse> response = noteService.findAll(mockUser, title, pageable);

            assertNotNull(response);
            assertEquals(1, response.getTotalElements());
            assertEquals(1, response.getTotalPages());

            verify(noteRepository).findAllByUserAndTitleContainingIgnoreCase(mockUser, title, pageable);
            verify(noteRepository, never()).findAllByUser(any(User.class), any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("Method: create()")
    class createMethod {

        @Test
        @DisplayName("Should create new note when data is valid")
        void shouldCreateNewNote_WhenDataIsValid() {
            String title = "new note";
            String content = "new content description";
            CreateNoteRequest request = new CreateNoteRequest(title, content);
            User mockUser = User.builder().id(UUID.randomUUID()).email("test@test.com").build();
            Note newNote = Note.builder().id(UUID.randomUUID()).title(request.title()).content(request.content()).user(mockUser).build();

            when(noteRepository.saveAndFlush(any(Note.class))).thenReturn(newNote);

            CreateNoteResponse response = noteService.create(request, mockUser);

            assertNotNull(response);
            assertNotNull(response.message());
            assertEquals(title, response.data().title());
            assertEquals(content, response.data().content());

            var captor = ArgumentCaptor.forClass(Note.class);
            verify(noteRepository).saveAndFlush(captor.capture());

            var capturedNote = captor.getValue();
            assertEquals(mockUser, capturedNote.getUser());
        }
    }

    @Nested
    @DisplayName("Method: updated()")
    class updatedMethod {

        @Test
        @DisplayName("Should update note when data is valid")
        void shouldUpdateNote_WhenDataIsValid() {
            String title = "New title updated";
            String content = "New content updated";
            UpdateNoteRequest request = new UpdateNoteRequest(title, content);
            Note mockNote = Note.builder().title("title").content("content").build();
            User mockUser = User.builder().email("test@test.com").build();

            when(noteRepository.findByIdAndUser(any(UUID.class), any(User.class))).thenReturn(Optional.of(mockNote));
            when(noteRepository.save(any(Note.class))).thenReturn(mockNote);

            UpdateNoteResponse response = noteService.update(UUID.randomUUID(), request, mockUser);

            assertNotNull(response);
            assertNotNull(response.message());
            assertEquals(title, response.data().title());
            assertEquals(content, response.data().content());
        }

        @Test
        @DisplayName("Should throw NotFoundException when note nonexist")
        void shouldThrowNotFoundException_WhenNoteNonExist() {
            UpdateNoteRequest request = new UpdateNoteRequest("title", "content");
            User mockUser = User.builder().email("test@test.com").build();

            when(noteRepository.findByIdAndUser(any(UUID.class), any(User.class))).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () -> noteService.update(UUID.randomUUID(), request, mockUser));

            verify(noteRepository, never()).save(any(Note.class));
        }
    }

    @Nested
    @DisplayName("Method: toggleCompleted()")
    class MethodToggleCompleted {

        @Test
        @DisplayName("Should toggle completed status to true when is false")
        void shouldToggleCompletedToTrue_WhenIsFalse() {
            Note mockNote = Note.builder().completed(false).build();
            User mockUser = User.builder().email("test@test").build();

            when(noteRepository.findByIdAndUser(any(UUID.class), any(User.class))).thenReturn(Optional.of(mockNote));
            when(noteRepository.save(any(Note.class))).thenReturn(mockNote);

            ToggleCompletedResponse response = noteService.toggleCompleted(UUID.randomUUID(), mockUser);

            assertNotNull(response);
            assertNotNull(response.message());
            assertTrue(response.data().completed());
        }

        @Test
        @DisplayName("Should throw NotFoundException when note nonexist")
        void shouldThrowNotFoundException_WhenNoteNonExist() {
            User mockUser = User.builder().email("test@test.com").build();

            when(noteRepository.findByIdAndUser(any(UUID.class), any(User.class))).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () -> noteService.toggleCompleted(UUID.randomUUID(), mockUser));

            verify(noteRepository, never()).save(any(Note.class));
        }
    }

    @Nested
    @DisplayName("Method: delete()")
    class deleteMethod {

        @Test
        @DisplayName("Should delete note when note exist")
        void shouldDeleteNote_WhenNoteExist() {
            Note mockNote = Note.builder().title("title").content("content").build();
            User mockUser = User.builder().email("test@test.com").build();

            when(noteRepository.findByIdAndUser(any(UUID.class), any(User.class))).thenReturn(Optional.of(mockNote));

            noteService.delete(UUID.randomUUID(), mockUser);

            verify(noteRepository).delete(mockNote);

        }

        @Test
        @DisplayName("Should throw NotFoundException when note nonexist")
        void shouldThrowNotFoundException_WhenNoteNonExist() {
            User mockUser = User.builder().email("test@test.com").build();

            when(noteRepository.findByIdAndUser(any(UUID.class), any(User.class))).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () -> noteService.delete(UUID.randomUUID(), mockUser));

            verify(noteRepository, never()).delete(any(Note.class));
        }
    }
}
