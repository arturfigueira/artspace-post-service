package com.artspace.post;

import io.smallrye.mutiny.Uni;
import java.util.List;
import java.util.Optional;
import javax.enterprise.context.ApplicationScoped;
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

  final Logger logger;


  /**
   * Permanently stores a new {@link Post} into the data repository. The post must contain valid
   * data, including an author that exists and is active, otherwise the post won't be saved
   * TODO Validate post before persistence
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
   * Retrieves a post by its unique object identifier
   * <p>
   * TODO validate the received id
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
   * Find all posts for specified user
   * @param author username of the author
   * @return A {@link Uni} that will resolve into a {@link List<Post>} for given author
   */
  public Uni<List<Post>> retrievePostsByAuthor(final String author) {
    return this.postRepo.list("author", author);
  }
}
