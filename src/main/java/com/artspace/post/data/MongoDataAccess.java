package com.artspace.post.data;

import com.artspace.post.Author;
import com.artspace.post.Post;
import io.smallrye.mutiny.Uni;
import java.util.Optional;
import javax.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;

/**
 * MongoDB implementation of a {@link PostDataAccess}
 */
@RequiredArgsConstructor
@ApplicationScoped
class MongoDataAccess implements PostDataAccess {

  final PostRepository postRepository;

  final AuthorReactiveRepository authorReactiveRepository;

  final PostSearchFactory postSearchFactory;

  @Override
  public Uni<Author> persist(Author author) {
    return this.authorReactiveRepository.persist(author);
  }

  @Override
  public Uni<Post> persist(Post post) {
    return this.postRepository.persist(post);
  }

  @Override
  public Uni<Optional<Author>> merge(final Author author) {
    return this.authorReactiveRepository.update("active", author.isActive())
        .where("username", author.getUsername())
        .map(aLong->Optional.of(author));
  }

  @Override
  public Uni<Optional<Post>> merge(Post post) {
    return this.postRepository.update(post).map(Optional::of);
  }

  @Override
  public Uni<Boolean> isAuthorActive(String username) {
    return this.authorReactiveRepository.find("username", username)
        .singleResultOptional()
        .map(result -> result.map(Author::isActive).orElse(false));
  }

  @Override
  public Uni<Optional<Post>> findById(String id) {
    return this.postRepository.findByIdOptional(new ObjectId(id));
  }

  @Override
  public Uni<Optional<Post>> findById(ObjectId id) {
    return this.postRepository.findByIdOptional(id);
  }

  @Override
  public Uni<Optional<Author>> findAuthorByUsername(String username) {
    return this.authorReactiveRepository.find("username", username)
        .singleResultOptional();
  }

  @Override
  public PaginatedSearch searchPosts() {
    return postSearchFactory.getNewInstance();
  }
}
