package com.notesapi.notes_api.note;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notesapi.notes_api.helpers.TestNoteHelper;
import com.notesapi.notes_api.helpers.TestUserHelper;
import com.notesapi.notes_api.note.dtos.CreateNoteRequest;
import com.notesapi.notes_api.note.dtos.CreateNoteResponse;
import com.notesapi.notes_api.note.dtos.UpdateNoteRequest;
import com.notesapi.notes_api.note.entities.Note;
import com.notesapi.notes_api.user.entities.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@Tag("integration")
@DisplayName("Integration tests - Notes")
@Transactional
public class NoteControllerTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgresSQL = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NoteRepository noteRepository;
    @Autowired
    private TestUserHelper testUserHelper;

    @Autowired
    private TestNoteHelper testNoteHelper;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String notesUrl = "/notes";

    private MultiValueMap<String, String> pageParams(int page, int size) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("page", String.valueOf(page));
        params.add("size", String.valueOf(size));
        return params;
    }

    @Nested
    @DisplayName("GET: /notes")
    class GetNotes {

        @Test
        @DisplayName("Should return all notes from user when authenticated")
        void shouldReturnAllNotesFromUser_WhenAuthenticated() throws Exception {
            User user = testUserHelper.createUser();
            testNoteHelper.createNote(user);
            testNoteHelper.createNote(user);
            testNoteHelper.createNote(user);

            mockMvc.perform(get(notesUrl)
                    .with(user(user)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content", hasSize(3)))
                    .andExpect(jsonPath("$.page").exists())
                    .andExpect(jsonPath("$.size").exists())
                    .andExpect(jsonPath("$.totalElements").exists())
                    .andExpect(jsonPath("$.totalPages").exists())
                    .andExpect(jsonPath("$.isLast").isBoolean());
        }

        @Test
        @DisplayName("Should return all notes with pagination from user when authenticated")
        void shouldReturnAllNotesWithPaginationFromUser_WhenAuthenticated() throws Exception {
            User user = testUserHelper.createUser();
            for (int i = 0; i < 10; i++) {
                testNoteHelper.createNote(user);
            }

            mockMvc.perform(get(notesUrl)
                    .queryParams(pageParams(1, 5))
                    .with(user(user)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content", hasSize(5)))
                    .andExpect(jsonPath("$.page").value(1))
                    .andExpect(jsonPath("$.size").value(5))
                    .andExpect(jsonPath("$.totalElements").value(10))
                    .andExpect(jsonPath("$.totalPages").value(2))
                    .andExpect(jsonPath("$.isLast").value(true));
        }

        @Test
        @DisplayName("Should return note with title searched when authenticated")
        void shouldReturnNoteWithTitleSearched_WhenAuthenticated() throws Exception {
            User user = testUserHelper.createUser();
            String title = "search by this note";
            for (int i = 0; i < 4; i++) {
                testNoteHelper.createNote(user);
            }
            testNoteHelper.title(title).createNote(user);

            mockMvc.perform(get(notesUrl)
                    .queryParam("title", title)
                    .with(user(user)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].title", containsString(title)))
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.size").value(10))
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.totalPages").value(1))
                    .andExpect(jsonPath("$.isLast").value(true));
        }

        @Test
        @DisplayName("Should return empty content when page is out of bounds")
        void shouldReturnEmptyContent_WhenPageIsOutOfBounds() throws Exception {
            User user = testUserHelper.createUser();
            testNoteHelper.createNote(user);

            mockMvc.perform(get(notesUrl)
                    .queryParams(pageParams(99, 10))
                    .with(user(user)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content", hasSize(0)))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("Should return empty content when title is not found")
        void shouldReturnEmptyContent_WhenTitleNotFound() throws Exception {
            User user = testUserHelper.createUser();
            testNoteHelper.createNote(user);

            mockMvc.perform(get(notesUrl)
                    .queryParam("title", "this title does not exist")
                    .with(user(user)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content", hasSize(0)))
                    .andExpect(jsonPath("$.totalElements").value(0));
        }

        @Test
        @DisplayName("Should normalize page to 0 when page is negative")
        void shouldNormalizePage_WhenPageIsNegative() throws Exception {
            User user = testUserHelper.createUser();

            mockMvc.perform(get(notesUrl)
                    .queryParams(pageParams(-1, 10))
                    .with(user(user)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.page").value(0));
        }

        @Test
        @DisplayName("Should normalize size to default when size is 0")
        void shouldNormalizeSize_WhenSizeIs0() throws Exception {
            User user = testUserHelper.createUser();

            mockMvc.perform(get(notesUrl)
                    .queryParams(pageParams(0, 0))
                    .with(user(user)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.size").value(10));
        }

        @Test
        @DisplayName("Should return status 401 when non authenticated")
        void shouldReturnStatus401_WhenNonAuthenticated() throws Exception {
            mockMvc.perform(get(notesUrl))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.error").exists())
                    .andExpect(jsonPath("$.timestamp").exists());
        }
    }

    @Nested
    @DisplayName("POST: /notes")
    class PostNotes {

        @Test
        @DisplayName("Should create a new note when authenticated")
        void shouldCreateANewNote_WhenAuthenticated() throws Exception {
            String title = "new title";
            String content = "new Content";
            CreateNoteRequest request = new CreateNoteRequest(title, content);
            String jsonRequest = objectMapper.writeValueAsString(request);
            User user = testUserHelper.createUser();

            String responseBody = mockMvc.perform(post(notesUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonRequest)
                    .with(user(user)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.data.id").exists())
                    .andExpect(jsonPath("$.data.user_id").doesNotExist())
                    .andExpect(jsonPath("$.data.title").value(title))
                    .andExpect(jsonPath("$.data.content").value(content))
                    .andExpect(jsonPath("$.data.completed").isBoolean())
                    .andExpect(jsonPath("$.data.createdAt").exists())
                    .andExpect(jsonPath("$.data.updatedAt").exists())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            var responseObj = objectMapper.readValue(responseBody, CreateNoteResponse.class);
            UUID noteId = responseObj.data().id();

            Note savedNote = noteRepository.findById(noteId).orElseThrow();
            assertEquals(title, savedNote.getTitle());
            assertEquals(content, savedNote.getContent());
            assertEquals(user.getId(), savedNote.getUser().getId());
        }

        @Test
        @DisplayName("Should return status 400 when request body is invalid")
        void shouldReturnStatus400_WhenRequestBodyIsInvalid() throws Exception {
            CreateNoteRequest request = new CreateNoteRequest(" ", " ");
            String jsonRequest = objectMapper.writeValueAsString(request);
            User user = testUserHelper.createUser();

            mockMvc.perform(post(notesUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonRequest)
                    .with(user(user)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.error").exists())
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.details", hasItem(containsString("title"))))
                    .andExpect(jsonPath("$.details", hasItem(containsString("content"))));
        }

        @Test
        @DisplayName("Should return status 401 when non authenticated")
        void shouldReturnStatus401_WhenNonAuthenticated() throws Exception {
            CreateNoteRequest request = new CreateNoteRequest("new title", "new content");
            String jsonRequest = objectMapper.writeValueAsString(request);

            mockMvc.perform(post(notesUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonRequest))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.error").exists())
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.status").value(401));
        }
    }

    @Nested
    @DisplayName("PATCH: /notes/{id}")
    class PatchNotes {

        @Test
        @DisplayName("Should update note when authenticated")
        void shouldUpdateNote_WhenAuthenticated() throws Exception {
            String title = "updated title";
            String content = "large content with much chars";
            User user = testUserHelper.createUser();

            Note note = testNoteHelper.title("New title").createNote(user);
            UpdateNoteRequest request = new UpdateNoteRequest(title, content);
            String jsonRequest = objectMapper.writeValueAsString(request);

            mockMvc.perform(patch(notesUrl + "/" + note.getId())
                    .with(user(user))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonRequest))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.data.id").value(note.getId().toString()))
                    .andExpect(jsonPath("$.data.title").value(title))
                    .andExpect(jsonPath("$.data.content").value(content))
                    .andExpect(jsonPath("$.data.completed").isBoolean())
                    .andExpect(jsonPath("$.data.createdAt").exists())
                    .andExpect(jsonPath("$.data.updatedAt").exists());

            Note updatedNote = noteRepository.findById(note.getId()).orElseThrow();
            assertEquals(title, updatedNote.getTitle());
        }

        @Test
        @DisplayName("Should return status 400 when request body is invalid")
        void shouldReturnStatus400_WhenRequestBodyIsInvalid() throws Exception {
            User user = testUserHelper.createUser();
            Note note = testNoteHelper.createNote(user);
            UpdateNoteRequest request = new UpdateNoteRequest(" ", " ");
            String jsonRequest = objectMapper.writeValueAsString(request);

            mockMvc.perform(patch(notesUrl + "/" + note.getId())
                    .with(user(user))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonRequest))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.error").exists())
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.details", hasItem(containsString("title"))))
                    .andExpect(jsonPath("$.details", hasItem(containsString("content"))));
        }

        @Test
        @DisplayName("Should return status 401 when non authenticated")
        void shouldReturnStatus401_WhenNonAuthenticated() throws Exception {

            mockMvc.perform(patch(notesUrl + "/" + UUID.randomUUID())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.error").exists())
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.status").value(401));
        }

        @Test
        @DisplayName("Should return status 404 (not 403) when user tries to update a note that does not belong to them")
        void shouldReturnStatus404_WhenAttackerTriesToUpdateNoteThatDoesNotBelongToHim() throws Exception {
            User victim = testUserHelper.email("victim@test.com").createUser();
            User attacker = testUserHelper.email("attacker@test.com").createUser();
            Note note = testNoteHelper.createNote(victim);
            UpdateNoteRequest request = new UpdateNoteRequest("updated title", "updated content");
            String jsonRequest = objectMapper.writeValueAsString(request);

            mockMvc.perform(patch(notesUrl + "/" + note.getId())
                    .with(user(attacker))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonRequest))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.error").exists())
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.status").value(404));
        }

        @Test
        @DisplayName("Should return status 404 when note does not exist")
        void shouldReturnStatus404_WhenNoteDoesNotExist() throws Exception {
            User user = testUserHelper.createUser();
            UpdateNoteRequest request = new UpdateNoteRequest("updated title", "updated content");
            String jsonRequest = objectMapper.writeValueAsString(request);

            mockMvc.perform(patch(notesUrl + "/" + UUID.randomUUID())
                    .with(user(user))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonRequest))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.error").exists())
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.status").value(404));
        }
    }

    @Nested
    @DisplayName("PATCH: /notes/{id}/toggle")
    class PatchNotesToggle {

        @Test
        @DisplayName("Should toggle note completed to true when authenticated")
        void shouldToggleNoteCompletedToTrue_WhenAuthenticated() throws Exception {
            User user = testUserHelper.createUser();
            Note note = testNoteHelper.createNote(user); // default completed value is false

            mockMvc.perform(patch(notesUrl + "/" + note.getId() + "/toggle")
                    .with(user(user)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.data.id").value(note.getId().toString()))
                    .andExpect(jsonPath("$.data.completed").value(true));

            Note updatedNote = noteRepository.findById(note.getId()).orElseThrow();
            assertTrue(updatedNote.isCompleted());
        }

        @Test
        @DisplayName("Should toggle note completed to false when authenticated")
        void shouldToggleNoteCompletedToFalse_WhenAuthenticated() throws Exception {
            User user = testUserHelper.createUser();
            Note note = testNoteHelper.completed(true).createNote(user);

            mockMvc.perform(patch(notesUrl + "/" + note.getId() + "/toggle")
                    .with(user(user)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.data.id").value(note.getId().toString()))
                    .andExpect(jsonPath("$.data.completed").value(false));

            Note updatedNote = noteRepository.findById(note.getId()).orElseThrow();
            assertFalse(updatedNote.isCompleted());
        }

        @Test
        @DisplayName("Should return status 401 when non authenticated")
        void shouldReturnStatus401_WhenNonAuthenticated() throws Exception {
            mockMvc.perform(patch(notesUrl + "/" + UUID.randomUUID() + "/toggle"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.error").exists())
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.status").value(401));
        }

        @Test
        @DisplayName("Should return status 404 when note does not exist")
        void shouldReturnStatus404_WhenNoteDoesNotExist() throws Exception {
            User user = testUserHelper.createUser();
            mockMvc.perform(patch(notesUrl + "/" + UUID.randomUUID() + "/toggle")
                    .with(user(user)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.error").exists())
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.status").value(404));
        }

        @Test
        @DisplayName("Should return status 404 (not 403) when user tries to toggle a note that does not belong to them")
        void shouldReturnStatus404_WhenAttackerTriesToToggleNoteThatDoesNotBelongToHim() throws Exception {
            User victim = testUserHelper.email("victim@test.com").createUser();
            User attacker = testUserHelper.email("attacker@test.com").createUser();
            Note note = testNoteHelper.createNote(victim);

            mockMvc.perform(patch(notesUrl + "/" + note.getId() + "/toggle")
                    .with(user(attacker)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.error").exists())
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.status").value(404));
        }
    }

    @Nested
    @DisplayName("DELETE: /notes/{id}")
    class DeleteNotes {

        @Test
        @DisplayName("Should delete note when authenticated")
        void shouldDeleteNote_WhenAuthenticated() throws Exception {
            User user = testUserHelper.createUser();
            Note note = testNoteHelper.createNote(user);

            mockMvc.perform(delete(notesUrl + "/" + note.getId())
                    .with(user(user)))
                    .andExpect(status().isNoContent());

            Note deletedNote = noteRepository.findById(note.getId()).orElse(null);
            assertNull(deletedNote);
        }

        @Test
        @DisplayName("Should return status 401 when non authenticated")
        void shouldReturnStatus401_WhenNonAuthenticated() throws Exception {
            mockMvc.perform(delete(notesUrl + "/" + UUID.randomUUID()))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.error").exists())
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.status").value(401));
        }

        @Test
        @DisplayName("Should return status 404 when note does not exist")
        void shouldReturnStatus404_WhenNoteDoesNotExist() throws Exception {
            User user = testUserHelper.createUser();
            mockMvc.perform(delete(notesUrl + "/" + UUID.randomUUID())
                    .with(user(user)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.error").exists())
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.status").value(404));
        }

        @Test
        @DisplayName("Should return status 404 (not 403) when user tries to delete a note that does not belong to them")
        void shouldReturnStatus404_WhenAttackerTriesToNoteThatDoesNotBelongToHim() throws Exception {
            User victim = testUserHelper.email("victim@test.com").createUser();
            User attacker = testUserHelper.email("attacker@test.com").createUser();
            Note note = testNoteHelper.createNote(victim);

            mockMvc.perform(delete(notesUrl + "/" + note.getId())
                    .with(user(attacker)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.error").exists())
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.status").value(404));
        }
    }
}
