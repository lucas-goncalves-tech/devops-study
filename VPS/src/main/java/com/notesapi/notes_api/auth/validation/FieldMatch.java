package com.notesapi.notes_api.auth.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(FieldMatch.List.class)
@Constraint(validatedBy = FieldMatchValidator.class)
public @interface FieldMatch {
    String field();
    String fieldMatch();
    String message() default "Os campos não conferem";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};


    @Target({ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @interface List {
        FieldMatch[] value();
    }
}
