package com.cineconecta.catalogo.dto;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

/** La lista no tiene dos elementos con el mismo número. Los nulos los reportan otras validaciones. */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = UniqueNumbers.Validator.class)
public @interface UniqueNumbers {

    String message() default "hay números repetidos";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<UniqueNumbers, List<? extends Numbered>> {

        @Override
        public boolean isValid(List<? extends Numbered> items, ConstraintValidatorContext context) {
            if (items == null) {
                return true;
            }
            Set<Integer> seen = new HashSet<>();
            return items.stream().filter(Objects::nonNull).map(Numbered::number).filter(Objects::nonNull)
                    .allMatch(seen::add);
        }
    }
}
