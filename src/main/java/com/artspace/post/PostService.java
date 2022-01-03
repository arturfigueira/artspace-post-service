package com.artspace.post;

import io.smallrye.mutiny.Uni;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import javax.enterprise.context.ApplicationScoped;
import javax.validation.Valid;
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
   * Update the stored data of a given {@link Post} This method update only the post's message and
   * enable/disable it.
   *
   * @param updatedPost post to be updated
   * @return A {@link Uni} that will resolve into the updated post. If post is not found the uni
   * will resolve into a {@code Optional.empty()}
   */
  public Uni<Optional<Post>> updatePost(final Post updatedPost) {
    return this.postRepo.findByIdOptional(updatedPost.getId())
        .chain(foundPost -> {
          Uni<Optional<Post>> result = Uni.createFrom().item(Optional.empty());

          if (foundPost.isPresent()) {
            Post postToUpdate = foundPost.get();
            postToUpdate.setEnabled(updatedPost.isEnabled());
            postToUpdate.setMessage(updatedPost.getMessage());
            result = this.postRepo.update(postToUpdate).map(Optional::of);
          }

          return result;
        });
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
   * Search posts. The search is done over an active user. A filter by post status can be applied.
   * <p>
   * This is a paginated search, requiring the current page and how many items must be returned.
   * This is mandatory due to performance, as the number of posts per user can grow indefinitely,
   * being impractical to return all posts of a user.
   * <p>
   * Returned posts will be automatically sorted by its creation date
   * <p>
   * Beware that posts will be returned only if the author is an <b>enabled user</b>, otherwise an
   * empty list will resolve as result.
   *
   * @param postQuery Search query parameters
   * @return A {@link Uni} that will resolve into a {@link List<Post>} for given author
   */
  public Uni<List<Post>> searchPosts(final PostQuery postQuery) {

    final var query = Optional.ofNullable(postQuery)
        .orElseThrow(() -> new IllegalArgumentException("PostQuery cannot be null"));

    return isAuthorActive(query.getAuthor())
        .chain(this.performSearch(query));
  }

  private Function<Boolean, Uni<? extends List<Post>>> performSearch(final PostQuery postQuery) {
    return authorIsActive -> Boolean.TRUE.equals(authorIsActive)
        ? this.executeSearch(postQuery)
        : this.getEmptyListOfPosts(postQuery.getAuthor());
  }

  private Uni<? extends List<Post>> getEmptyListOfPosts(String author) {
    logger.tracef("User %s is nonexistent or inactive. Empty post list returned", author);
    return Uni.createFrom().item(Collections.emptyList());
  }

  private Uni<? extends List<Post>> executeSearch(final PostQuery postQuery) {
    return this.postRepo
        .find(postQuery.getQuery(), postQuery.getSort(), postQuery.getParameters())
        .page(postQuery.getPage().getIndex(), postQuery.getPage().getSize())
        .list();
  }
}
