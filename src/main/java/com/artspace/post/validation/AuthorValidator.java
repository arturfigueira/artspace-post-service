package com.artspace.post.validation;

import io.smallrye.common.annotation.Blocking;
import javax.enterprise.context.ApplicationScoped;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;

/**
 * Constraint validator to guarantee that the annotated username exists in the application and is
 * an active user.
 */
@RequiredArgsConstructor
@ApplicationScoped
class AuthorValidator implements ConstraintValidator<ExistsAndActive, String> {

  final AuthorValidatorService authorValidatorService;

  @Blocking
  @Override
  public boolean isValid(String annotatedUserName,
      final ConstraintValidatorContext constraintValidatorContext) {
    return authorValidatorService.isAuthorValid(annotatedUserName);
  }
}
