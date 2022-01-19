package com.artspace.post;

import com.artspace.post.data.PaginatedSearch;
import com.artspace.post.data.PostDataAccess;
import io.smallrye.mutiny.Uni;
import java.util.Optional;
import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;
import javax.validation.Valid;
import lombok.AllArgsConstructor;

/**
 * Service class to provide access to user's post data
 *
 * @since 1.0.0
 */
@AllArgsConstructor
@ApplicationScoped
public class PostService {

  final PostDataAccess postDataAccess;

  private static Author normalizeAuthor(final Author input) {
    return input.withUsername(normalizeUserName(input.getUsername()));
  }

  private static String normalizeUserName(String username) {
    return username.toLowerCase().trim();
  }

  private static Optional<String> nonBlank(String username) {
    return Optional.ofNullable(username)
        .filter(value -> !value.isBlank());
  }

  private static Author copyTo(final Author source, final Author receiver) {
    receiver.setActive(source.isActive());
    return receiver;
  }

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
    return this.postDataAccess.persist(normalizeAuthor(author));
  }

  /**
   * Update an author by its username
   *
   * @param author author to be updated
   */
  public Uni<Optional<Author>> updateAuthor(@Valid final Author author) {
    return this.postDataAccess.merge(normalizeAuthor(author));
  }


  /**
   * Find an {@link Author} entity by its username
   *
   * @param username username of the author
   * @return an {@link Uni} that will resolve it the found user or {@code Optional.empty()}
   * otherwise.
   */
  public Uni<Optional<Author>> findAuthorByUsername(final String username) {
    return nonBlank(username)
        .map(value -> this.postDataAccess.findAuthorByUsername(normalizeUserName(value)))
        .orElseGet(() -> Uni.createFrom().item(Optional.empty()));
  }

  /**
   * If given {@link Author} is already persisted the previous data will be updated. If this is a
   * new author, it will be inserted into the repository
   *
   * @param author to be updated or inserted into the repository
   * @return an {@link Uni} that will resolve it the updated/inserted author
   */
  @Transactional(Transactional.TxType.REQUIRES_NEW)
  public Uni<Optional<Author>> persistOrUpdateAuthor(@Valid final Author author) {
    return this.postDataAccess.findAuthorByUsername(author.getUsername())
        .chain(foundAuthor -> foundAuthor.map(a -> this.postDataAccess.merge(copyTo(author, a)))
            .orElseGet(() -> this.postDataAccess.persist(author).map(Optional::ofNullable)));
  }


  /**
   * Verify if given author, by its username, exists and is active at the moment
   *
   * @param username username of the desired author
   * @return {@code true} if the author exists and is active, {@code false} otherwise
   */
  protected Uni<Boolean> isAuthorActive(String username) {
    return nonBlank(username)
        .map(value -> this.postDataAccess.isAuthorActive(username))
        .orElseGet(() -> Uni.createFrom()
            .item(false));
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
    return this.postDataAccess.persist(normalizedPost);
  }


  /**
   * Update the stored data of a given {@link Post} This method update only the post's message and
   * enable/disable it.
   *
   * @param updatedPost post to be updated
   * @return A {@link Uni} that will resolve into the updated post. If post is not found the uni
   * will resolve into a {@code Optional.empty()}
   */
  @Transactional(Transactional.TxType.REQUIRES_NEW)
  public Uni<Optional<Post>> updatePost(final Post updatedPost) {
    return this.postDataAccess.findById(updatedPost.getId())
        .chain(foundPost -> {
          Uni<Optional<Post>> result = Uni.createFrom().item(Optional.empty());

          if (foundPost.isPresent()) {
            Post postToUpdate = foundPost.get();
            postToUpdate.setEnabled(updatedPost.isEnabled());
            postToUpdate.setMessage(updatedPost.getMessage());
            result = this.postDataAccess.merge(postToUpdate);
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
    return this.postDataAccess.findById(id);
  }


  /**
   * Initiate a {@link PaginatedSearch} pipeline
   *
   * @return a new instance of a paginated search
   */
  public PaginatedSearch searchPosts() {
    return this.postDataAccess.searchPosts();
  }
}
