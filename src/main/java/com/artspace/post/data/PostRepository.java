package com.artspace.post.data;

import com.artspace.post.Post;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepository;
import io.smallrye.mutiny.Uni;
import java.util.List;
import java.util.stream.Collectors;
import javax.enterprise.context.ApplicationScoped;
import org.bson.types.ObjectId;

@ApplicationScoped
public class PostRepository implements ReactivePanacheMongoRepository<Post> {

  public Uni<List<Post>> findByIds(final List<ObjectId> ids) {
    return find("_id in ?1", ids).list();
  }
}
