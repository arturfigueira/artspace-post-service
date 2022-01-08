package com.artspace.post.data;

import com.artspace.post.Post;
import io.quarkus.mongodb.reactive.ReactiveMongoClient;
import io.smallrye.mutiny.Uni;
import java.util.List;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.bson.Document;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Query posts by author and post status. Due to {@code Author}  being a separated document at mongo
 * a $lookup aggregation query is required, to join those two documents and apply specified filters
 * and sorting. Posts will automatically be filtered by enabled authors only. Posts from disabled
 * authors will be automatically removed, even when this user is the filter author
 * <p>
 * For more information on lookup syntax, see <a href="https://docs.mongodb.com/manual/reference/operator/aggregation/lookup/#mongodb-pipeline-pipe.-lookup">
 * the $lookup pipeline doc</a>
 * <p>
 * obs: Due to a limitation with quarkus/cdi injection all injected properties won't be final nor
 * private. Injection of config properties were not possible via a constructor. Therefore, injection
 * will be done via property injection, instead of constructor injection.
 */
@Named("author.query")
@ApplicationScoped
public class AuthorLookupQuery implements PostQuery {

  private static final String LOOKUP_JOIN_AS = "postAuthor";

  @Inject
  ReactiveMongoClient mongoClient;

  @ConfigProperty(name = "quarkus.mongodb.database")
  String database;

  public Uni<List<Post>> invoke(final PaginatedSearch paginatedSearch) {
    final var author = paginatedSearch.getAuthorFilter()
        .orElseThrow(
            () -> new IllegalArgumentException("Author filter must be set for Lookup Search"));

    final var lookup = new Document("$lookup",
        new Document("from", "author")
            .append("localField", "username")
            .append("foreignField", "username")
            .append("as", LOOKUP_JOIN_AS));

    final var matchParameters = new Document(LOOKUP_JOIN_AS + ".active", true)
        .append(LOOKUP_JOIN_AS + ".username", author);

    paginatedSearch.getPostStatusFilter()
        .ifPresent(value -> matchParameters.append("enabled", value));

    final var match = new Document("$match", matchParameters);
    final var sort = new Document("$sort", new Document(paginatedSearch.getSortBy(), -1L));
    final var skip = new Document("$skip", paginatedSearch.skipUntil());
    final var limit = new Document("$limit", paginatedSearch.limitTo());

    final var pipeline = List.of(lookup, match, sort, skip, limit);
    final var reactiveCollection = this.mongoClient
        .getDatabase(this.database)
        .getCollection("post", Post.class)
        .aggregate(pipeline);

    return reactiveCollection.onItem().transform(post -> post).collect().asList();
  }
}