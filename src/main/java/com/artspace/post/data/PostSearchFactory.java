package com.artspace.post.data;

import com.artspace.post.Post;
import io.smallrye.mutiny.Uni;
import java.util.List;
import java.util.function.Function;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Factory class to build and configure new instances of {@link PaginatedSearch}. Instances created
 * by this class will be initialized with default values for all properties , based on the
 * application's configuration.
 * <p>
 * Configurations for this factory are under the prefix {@code post.search.*}, found at {@code
 * application.properties} file.
 * <p>
 * Due to a limitation of quarkus/cdi injection all injected properties won't be final nor private.
 * Injection of config properties were not possible via a constructor. Therefore, injection * will
 * be done via property injection, instead of constructor injection.
 */
@ApplicationScoped
class PostSearchFactory {

  @ConfigProperty(name = "post.search.pagination.page", defaultValue = "0")
  int defaultPageIndex;

  @ConfigProperty(name = "post.search.pagination.size", defaultValue = "20")
  int defaultPageSize;

  @ConfigProperty(name = "post.search.sort.by", defaultValue = "creationTime")
  String defaultSort;

  @ConfigProperty(name = "post.search.filter.status", defaultValue = "all")
  String defaultStatus;

  @Inject
  PostRepository postRepository;

  @Named("author.query")
  PostQuery lookupQuery;

  @Named("status.query")
  PostQuery postStatusFindQuery;

  PaginatedSearch getNewInstance() {
    final Function<PaginatedSearch, Uni<List<Post>>> searchCallback =
        (PaginatedSearch ps) -> (ps.getAuthorFilter().isPresent() ? lookupQuery : postStatusFindQuery).
            invoke(ps);

    return new PaginatedSearch(searchCallback)
        .pageSize(defaultPageSize)
        .atPage(defaultPageIndex)
        .sortedBy(defaultSort)
        .byPostStatus(defaultStatus);
  }
}
