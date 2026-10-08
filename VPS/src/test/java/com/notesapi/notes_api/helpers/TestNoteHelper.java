package com.notesapi.notes_api.helpers;

import com.notesapi.notes_api.note.NoteRepository;
import com.notesapi.notes_api.note.entities.Note;
import com.notesapi.notes_api.user.entities.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TestNoteHelper {

    private final NoteRepository noteRepository;

    private String title = "New note";
    private String content = "new content";
    private boolean completed = false;

    public TestNoteHelper title(String title) {
        this.title = title;
        return this;
    }

    public TestNoteHelper content(String content) {
        this.content = content;
        return this;
    }

    public TestNoteHelper completed(boolean completed) {
        this.completed = completed;
        return this;
    }

    public Note createNote(User user) {
        Note note = Note.builder().title(title).content(content).user(user).completed(completed).build();
        return noteRepository.save(note);
    }
}
