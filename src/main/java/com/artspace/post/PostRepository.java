package com.artspace.post;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepository;
import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
class PostRepository implements ReactivePanacheMongoRepository<Post> {

}
