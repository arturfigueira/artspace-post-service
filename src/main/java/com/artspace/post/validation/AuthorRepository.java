package com.artspace.post.validation;

import com.artspace.post.Author;
import io.quarkus.mongodb.panache.PanacheMongoRepository;
import javax.enterprise.context.ApplicationScoped;

/**
 * Blocking {@link Author} repository
 */
@ApplicationScoped
class AuthorRepository implements PanacheMongoRepository<Author> {

}
