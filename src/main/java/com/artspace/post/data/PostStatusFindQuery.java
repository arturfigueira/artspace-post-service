package com.artspace.post.data;

import com.artspace.post.Post;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;
import java.util.List;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

/**
 * Query posts by post status.
 */
@Named("status.query")
@ApplicationScoped
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class PostStatusFindQuery implements PostQuery {

  final PostRepository postRepository;

  @Override
  public Uni<List<Post>> invoke(final PaginatedSearch paginatedSearch) {
    final var postStatusFilter = paginatedSearch.getPostStatusFilter();
    final var sort = Sort.by(paginatedSearch.getSortBy());

    return postStatusFilter
        .map(value -> postRepository.find("enabled", sort, value))
        .orElseGet(() -> postRepository.findAll(sort))
        .page(paginatedSearch.getPage(), paginatedSearch.getSize())
        .list();
  }
}