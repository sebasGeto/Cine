package com.cineconecta.catalogo.dto;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import java.util.Set;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

/**
 * Campos obligatorios y admitidos según el tipo, igual que las tres variantes (oneOf) del
 * validador de la colección contents. Cada problema se reporta en su propio campo.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ContentFieldsByType.Validator.class)
public @interface ContentFieldsByType {

    String message() default "no es válido para este tipo de contenido";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<ContentFieldsByType, ContentRequest> {

        private static final Set<Integer> AGE_RATINGS = Set.of(0, 7, 12, 16, 18);

        @Override
        public boolean isValid(ContentRequest r, ConstraintValidatorContext context) {
            context.disableDefaultConstraintViolation();
            Check check = new Check(context, r.type());

            if (r.ageRating() != null && !AGE_RATINGS.contains(r.ageRating())) {
                check.fail("ageRating", "debe ser 0, 7, 12, 16 o 18");
            }
            // Un tipo nulo o desconocido ya lo reporta @Pattern
            switch (r.type() == null ? "" : r.type()) {
                case "movie" -> {
                    check.required("durationSec", r.durationSec());
                    check.notEmpty("directors", r.directors());
                    check.required("cast", r.cast());
                    check.forbidden("topics", r.topics());
                    check.forbidden("interviewees", r.interviewees());
                    check.forbidden("sources", r.sources());
                    check.forbidden("seasons", r.seasons());
                }
                case "documentary" -> {
                    check.required("durationSec", r.durationSec());
                    check.notEmpty("directors", r.directors());
                    check.notEmpty("topics", r.topics());
                    check.forbidden("cast", r.cast());
                    check.forbidden("seasons", r.seasons());
                }
                case "series" -> {
                    check.forbidden("durationSec", r.durationSec());
                    check.forbidden("directors", r.directors());
                    check.forbidden("cast", r.cast());
                    check.forbidden("topics", r.topics());
                    check.forbidden("interviewees", r.interviewees());
                    check.forbidden("sources", r.sources());
                }
                default -> { }
            }
            return check.valid;
        }

        private static final class Check {

            private final ConstraintValidatorContext context;
            private final String type;
            private boolean valid = true;

            Check(ConstraintValidatorContext context, String type) {
                this.context = context;
                this.type = type;
            }

            void required(String field, Object value) {
                if (value == null) {
                    fail(field, "es obligatorio en el tipo " + type);
                }
            }

            void notEmpty(String field, List<?> value) {
                if (value == null || value.isEmpty()) {
                    fail(field, "debe tener al menos uno en el tipo " + type);
                }
            }

            void forbidden(String field, Object value) {
                if (value != null) {
                    fail(field, "no corresponde al tipo " + type);
                }
            }

            void fail(String field, String message) {
                valid = false;
                context.buildConstraintViolationWithTemplate(message)
                        .addPropertyNode(field)
                        .addConstraintViolation();
            }
        }
    }
}
