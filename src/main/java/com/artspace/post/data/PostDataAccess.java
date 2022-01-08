package com.artspace.post.data;

import com.artspace.post.Author;
import com.artspace.post.Post;
import io.smallrye.mutiny.Uni;
import java.util.Optional;
import org.bson.types.ObjectId;

/**
 * Base structure to provide data access to externally stored {@link Post}
 */
public interface PostDataAccess {

  Uni<Author> persist(final Author author);

  Uni<Post> persist(final Post post);

  Uni<Long> merge(final Author author);

  Uni<Optional<Post>> merge(final Post post);

  Uni<Boolean> isAuthorActive(String username);

  Uni<Optional<Post>> findById(final String id);

  Uni<Optional<Post>> findById(final ObjectId id);

  PaginatedSearch searchPosts();
}
