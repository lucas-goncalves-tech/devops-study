package com.notesapi.notes_api.note;

import com.notesapi.notes_api.note.dtos.*;
import com.notesapi.notes_api.user.entities.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/notes")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Notas", description = "gestão de notas")
public class NoteController {

    private final NoteService noteService;

    @GetMapping
    @Operation(summary = "Lista todas as notas")
    public ResponseEntity<PageNoteResponse> findAll(@AuthenticationPrincipal User user,
                                                    @RequestParam(value = "title", required = false) String title,
                                                    @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) @ParameterObject Pageable pageable) {
        Page<NoteResponse> responsePage = noteService.findAll(user, title, pageable);
        return ResponseEntity.ok(PageNoteResponse.fromPageable(responsePage));
    }

    @PostMapping
    @Operation(summary = "Cria uma nova nota")
    public ResponseEntity<CreateNoteResponse> create(@Valid @RequestBody CreateNoteRequest request,
                                                     @AuthenticationPrincipal User user) {

        return ResponseEntity.status(HttpStatus.CREATED).body(noteService.create(request, user));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Atuliza uma nota")
    public ResponseEntity<UpdateNoteResponse> update(@PathVariable("id") UUID noteId,
                                                     @Valid @RequestBody UpdateNoteRequest request,
                                                     @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(noteService.update(noteId, request, user));
    }

    @PatchMapping("/{id}/toggle")
    @Operation(summary = "Define a nota como completada ou não completada")
    public ResponseEntity<ToggleCompletedResponse> toggleCompleted(@PathVariable("id") UUID noteId,
                                                                   @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(noteService.toggleCompleted(noteId, user));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deleta uma nota permanentemente")
    public ResponseEntity<Void> delete(@PathVariable("id") UUID noteId,
                                       @AuthenticationPrincipal User user) {
        noteService.delete(noteId, user);
        return ResponseEntity.noContent().build();
    }
}
