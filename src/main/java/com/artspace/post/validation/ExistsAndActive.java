package com.artspace.post.validation;

import static javax.validation.constraintvalidation.ValidationTarget.ANNOTATED_ELEMENT;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.validation.Constraint;
import javax.validation.Payload;
import javax.validation.constraintvalidation.SupportedValidationTarget;

@Documented
@Target({
    ElementType.FIELD,
    ElementType.PARAMETER,
    ElementType.ANNOTATION_TYPE
})
@Retention(RetentionPolicy.RUNTIME)
@SupportedValidationTarget(ANNOTATED_ELEMENT)
@Constraint(validatedBy = AuthorValidator.class)
public @interface ExistsAndActive {
  String message() default "user must exists and be an active user." +
      " validated username: ${validatedValue}";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
