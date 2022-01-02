package com.artspace.post;

import io.smallrye.mutiny.Uni;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import javax.enterprise.context.ApplicationScoped;
import javax.validation.ConstraintValidator;
import javax.validation.Valid;
import javax.validation.Validator;
import lombok.AllArgsConstructor;
import org.bson.types.ObjectId;
import org.jboss.logging.Logger;

/**
 * Service class to provide access to user's post data
 *
 * @since 1.0.0
 */
@AllArgsConstructor
@ApplicationScoped
class PostService {

  final PostRepository postRepo;

  final AuthorReactiveRepository authorReactiveRepository;

  final Logger logger;

  /**
   * Register a valid new Author into the repository. Author data will be normalized before being
   * registered, including setting the username to lowercase
   *
   * @param author author to register
   * @return a {@link Uni} that will resolve into the registered author, including the generated
   * identifier
   * @throws javax.validation.ConstraintViolationException if the specified author contains invalid
   *                                                       data
   */
  public Uni<Author> registerAuthor(@Valid final Author author) {
    final var normalizeAuthor = this.normalizeAuthor(author);
    return this.authorReactiveRepository.persist(normalizeAuthor);
  }

  /**
   * Update an author by its username
   *
   * @param author author to be updated
   * @return
   */
  public Uni<Long> updateAuthor(final Author author) {
    final var normalizedAuthor = this.normalizeAuthor(author);
    return this.authorReactiveRepository.update("active", false)
        .where("username", normalizedAuthor.getUsername());
  }

  private Author normalizeAuthor(final Author input) {
    return input.withUsername(input.getUsername().toLowerCase().trim());
  }

  /**
   * Verify if given author, by its username, exists and is active at the moment
   *
   * @param username username of the desired author
   * @return {@code true} if the author exists and is active, {@code false} otherwise
   */
  protected Uni<Boolean> isAuthorActive(String username) {
    return authorReactiveRepository.find("username", username)
        .singleResultOptional()
        .map(result -> result.map(Author::isActive).orElse(false));
  }

  /**
   * Permanently stores a new {@link Post} into the data repository. Data validations won't be done
   * at this service layer, and should be done by its caller
   *
   * @param post A post data to be persisted
   * @return An {@link Uni} that will be resolved into the persisted Post, including its newly
   * generated id
   */
  public Uni<Post> insertPost(final Post post) {
    final var normalizedPost = post.toToday();
    normalizedPost.enableIt();
    return this.postRepo.persist(normalizedPost);
  }

  /**
   * Retrieves a post by its unique object identifier. This will return results even when the author
   * is disabled
   * <p>
   * This method ignores the current author state.
   *
   * @param id unique object identifier
   * @return an {@link Uni} which will resolve into an {@link Optional<Post>}. {@code
   * Optional.empty()} represents that no post was found
   */
  public Uni<Optional<Post>> retrievePostById(final String id) {
    final var postId = new ObjectId(id);
    return this.postRepo.findByIdOptional(postId);
  }

  /**
   * Find all posts for a specified author. Posts will be returned only if the author is an enabled
   * user, otherwise an empty list will resolve as result.
   *
   * @param author username of the author
   * @return A {@link Uni} that will resolve into a {@link List<Post>} for given author
   */
  public Uni<List<Post>> retrievePostsByAuthor(final String author) {
    return isAuthorActive(author).chain(this.collectAuthorsPosts(author));
  }

  private Function<Boolean, Uni<? extends List<Post>>> collectAuthorsPosts(String author) {
    return authorIsActive -> {
      Uni<? extends List<Post>> response;
      if (Boolean.TRUE.equals(authorIsActive)) {
        response = this.postRepo.list("author", author);
      } else {
        response = Uni.createFrom().item(Collections.emptyList());
        logger.tracef("User %s is nonexistent or inactive. Empty post list returned", author);
      }

      return response;
    };
  }
}
