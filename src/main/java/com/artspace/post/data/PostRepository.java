package com.artspace.post.data;

import com.artspace.post.Post;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepository;
import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PostRepository implements ReactivePanacheMongoRepository<Post> {

}
