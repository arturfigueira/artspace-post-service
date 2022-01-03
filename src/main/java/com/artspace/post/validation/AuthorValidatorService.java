package com.artspace.post.validation;

import com.artspace.post.Author;
import java.util.Optional;
import javax.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;

/**
 * Service that provides auxiliary data access necessary to {@link AuthorValidator} effectively
 * validate an author
 */
@ApplicationScoped
@RequiredArgsConstructor
class AuthorValidatorService {

  final AuthorRepository blockingAuthorRepository;

  /**
   * Verifies if the author, by its username, exists and is active. Blank values will render this
   * validation to {@code false}.
   *
   * @param username author's username
   * @return {@code true} if the author exists and is active, {@code false} otherwise
   */
  public boolean isAuthorValid(String username) {
    Optional<Author> author = Optional.empty();

    if (Optional.ofNullable(username).isPresent()) {
      author = blockingAuthorRepository.find("username", username.toLowerCase().trim())
          .singleResultOptional();
    }

    return author.map(Author::isActive).orElse(false);
  }
}
