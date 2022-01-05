package com.artspace.post.data;

import com.artspace.post.Author;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepository;
import javax.enterprise.context.ApplicationScoped;

/**
 * Non-Blocking implementation of an {@link Author} repository
 */
@ApplicationScoped
class AuthorReactiveRepository implements ReactivePanacheMongoRepository<Author> {

}
