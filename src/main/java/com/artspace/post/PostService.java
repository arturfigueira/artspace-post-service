package com.artspace.post;

import com.artspace.post.data.PaginatedSearch;
import com.artspace.post.data.PostDataAccess;
import io.smallrye.mutiny.Uni;
import java.util.Optional;
import javax.enterprise.context.ApplicationScoped;
import javax.validation.Valid;
import lombok.AllArgsConstructor;
import org.jboss.logging.Logger;

/**
 * Service class to provide access to user's post data
 *
 * @since 1.0.0
 */
@AllArgsConstructor
@ApplicationScoped
class PostService {

  final PostDataAccess postDataAccess;
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
    return this.postDataAccess.persist(this.normalizeAuthor(author));
  }

  /**
   * Update an author by its username
   *
   * @param author author to be updated
   */
  public Uni<Long> updateAuthor(final Author author) {
    return this.postDataAccess.merge(this.normalizeAuthor(author));
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
    return this.postDataAccess.isAuthorActive(username);
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
   * @return a new instance of a paginated search
   */
  public PaginatedSearch searchPosts() {
    return this.postDataAccess.searchPosts();
  }
}
