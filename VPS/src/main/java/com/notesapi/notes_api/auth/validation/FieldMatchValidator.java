package com.notesapi.notes_api.auth.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.DirectFieldAccessor;

import java.util.Objects;

public class FieldMatchValidator implements ConstraintValidator<FieldMatch, Object> {

    private String firstFieldName;
    private String secondFieldName;
    private String message;

    @Override
    public void initialize(FieldMatch constraintAnnotation){
        this.firstFieldName = constraintAnnotation.field();
        this.secondFieldName = constraintAnnotation.fieldMatch();
        this.message = constraintAnnotation.message();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        try{
            final Object firstObj = new DirectFieldAccessor(value).getPropertyValue(this.firstFieldName);
            final Object secondObj = new DirectFieldAccessor(value).getPropertyValue(this.secondFieldName);

            boolean isValid = Objects.equals(firstObj, secondObj);

            if(!isValid){
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(message)
                        .addPropertyNode(secondFieldName)
                        .addConstraintViolation();
            }

            return isValid;
        } catch (final Exception e) {
            return false;
        }
    }
}
