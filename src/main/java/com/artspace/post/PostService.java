package com.artspace.post;

import com.artspace.post.data.PaginatedSearch;
import com.artspace.post.data.PostDataAccess;
import com.artspace.post.outgoing.Action;
import com.artspace.post.outgoing.DataEmitter;
import com.artspace.post.outgoing.PostDTO;
import io.smallrye.mutiny.Uni;
import java.time.Duration;
import java.util.Optional;
import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
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

  private static final Duration TIMEOUT = Duration.ofSeconds(2);

  final PostDataAccess postDataAccess;

  final PostMapper postMapper;

  final DataEmitter<PostDTO> emitter;

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
   * <p>
   * Due to a panache limitations, regarding transactions for mongodb, which is still experimental,
   * and it doesn't support reactive repositories, this method must be a blocking operation. Refer
   * to <a href="https://quarkus.io/guides/mongodb-panache#reactive">Quarkus Reactive Entities and
   * Repositories Guide-</a> for more details
   *
   * @param author to be updated or inserted into the repository
   * @return an {@link Uni} that will resolve it the updated/inserted author
   */
  @Transactional(TxType.REQUIRED)
  public Optional<Author> persistOrUpdateAuthor(@Valid final Author author) {
    final var foundAuthor = this.postDataAccess
        .findAuthorByUsername(author.getUsername())
        .await()
        .atMost(TIMEOUT);

    final Uni<Optional<Author>> result = foundAuthor.isPresent() ?
        this.postDataAccess.merge(copyTo(author, foundAuthor.get())) :
        this.postDataAccess.persist(author).map(Optional::ofNullable);

    return result.await().atMost(TIMEOUT);
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
   * at this service layer, and should be done by its caller.
   * <p>
   * If successfully persisted, a notification will be broadcast to a message broker to notify that
   * a new post has being introduced
   *
   * @param post A post data to be persisted
   * @param correlationId Transit id of the original request that made this insert necessary
   * @return An {@link Uni} that will be resolved into the persisted Post, including its newly
   * generated id
   */
  public Uni<Post> insertPost(final Post post, final String correlationId) {
    final var normalizedPost = post.toToday();
    normalizedPost.enableIt();
    return this.postDataAccess.persist(normalizedPost)
        .invoke(pPost -> this.broadcastPersist(pPost, correlationId));
  }


  /**
   * Update the stored data of a given {@link Post} This method update only the post's message and
   * enable/disable it.
   *
   * <p>
   * If successfully persisted, a notification will be broadcast to a message broker, notifying that
   * given post has being updated
   *
   * <p>
   * Due to a panache limitations, regarding transactions for mongodb, which is still experimental,
   * and it doesn't support reactive repositories, this method must be a blocking operation. Refer
   * to <a href="https://quarkus.io/guides/mongodb-panache#reactive">Quarkus Reactive Entities and
   * Repositories Guide-</a> for more details
   *
   * @param updatedPost post to be updated
   * @param correlationId Transit id of the original request that made this update necessary
   * @return A {@link Optional<Post>} that with the updated post. If post is not found a {@code
   * Optional.empty()} will be returned otherwise.
   */
  @Transactional(TxType.REQUIRED)
  public Optional<Post> updatePost(final Post updatedPost, final String correlationId) {
    final var foundPost = this.postDataAccess.
        findById(updatedPost.getId())
        .await()
        .atMost(TIMEOUT);

    Optional<Post> result = Optional.empty();

    if (foundPost.isPresent()) {
      Post postToUpdate = foundPost.get();
      postToUpdate.setEnabled(updatedPost.isEnabled());
      postToUpdate.setMessage(updatedPost.getMessage());
      result = this.postDataAccess.merge(postToUpdate)
          .invoke(p -> p.ifPresent(post -> this.broadcastUpdate(post, correlationId)))
          .await()
          .atMost(TIMEOUT);
    }
    return result;
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

  private void broadcastPersist(final Post post, final String correlationId) {
    this.broadcastChanges(post, correlationId, Action.CREATED);
  }

  private void broadcastUpdate(final Post post, final String correlationId) {
    this.broadcastChanges(post, correlationId, Action.UPDATED);
  }

  private void broadcastChanges(final Post post, final String correlationId, final Action action) {
    final var postDto = postMapper.toDTO(post);
    postDto.setAction(action);
    emitter.emit(correlationId, postDto);
  }
}
