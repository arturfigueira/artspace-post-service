package com.artspace.post;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepository;
import javax.enterprise.context.ApplicationScoped;

/**
 * Non-Blocking implementation of an {@link Author} repository
 */
@ApplicationScoped
class AuthorReactiveRepository implements ReactivePanacheMongoRepository<Author> {

}
