package com.artspace.post.data;

import com.artspace.post.Post;
import io.smallrye.mutiny.Uni;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Provide non-blocking {@link Post} searching with pagination and sorting
 */
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class PaginatedSearch {

  final Function<PaginatedSearch, Uni<List<Post>>> searchCallback;

  private String authorFilter = null;

  @Getter(AccessLevel.PROTECTED)
  private int page;

  @Getter(AccessLevel.PROTECTED)
  private int size;

  @Getter(AccessLevel.PROTECTED)
  private String sortBy;

  private PostStatus postStatus;

  long skipUntil() {
    return (long) this.page * this.size;
  }

  long limitTo() {
    return this.size;
  }

  Optional<String> getAuthorFilter() {
    return Optional.ofNullable(authorFilter);
  }

  Optional<Boolean> getPostStatusFilter() {
    return this.postStatus.getValue();
  }

  /**
   * Set the search page result
   *
   * @param pageIndex search result page
   * @return current instance of PaginatedSearch with updated page index
   * @throws IllegalArgumentException if specified page index is less than zero
   */
  public PaginatedSearch atPage(int pageIndex) {
    if (pageIndex < 0) {
      throw new IllegalArgumentException("Page index must be greater or equal zero");
    }

    this.page = pageIndex;
    return this;
  }

  /**
   * Limit the number of elements per search page
   *
   * @param pageSize max items per page
   * @return current instance of PaginatedSearch with updated page size
   * @throws IllegalArgumentException if specified page size is below or equals zero
   */
  public PaginatedSearch pageSize(int pageSize) {
    if (pageSize <= 0) {
      throw new IllegalArgumentException("Page size must be greater than zero");
    }
    this.size = pageSize;

    return this;
  }

  /**
   * Sort the results by a post field
   *
   * @param sortKey post field to sort the search result by
   * @return current instance of PaginatedSearch with updated sort
   * @throws IllegalArgumentException if sort key is blank or null
   */
  public PaginatedSearch sortedBy(String sortKey) {
    this.sortBy = Optional.ofNullable(sortKey).filter(s -> !s.isBlank())
        .orElseThrow(() -> new IllegalArgumentException("Sort key must no be null nor blank"));
    return this;
  }

  /**
   * Filter posts by an author. Adding an author filter will automatically exclude results from
   * authors that are disabled
   *
   * @param author username of the author
   * @return current instance of PaginatedSearch with added author filter
   */
  public PaginatedSearch byAuthor(String author) {
    this.authorFilter = author;
    return this;
  }

  /**
   * Filter posts by post status.
   *
   * @param postStatus post status
   * @return current instance of PaginatedSearch with added author filter
   * @throws IllegalArgumentException if status is blank, null or if given status is invalid
   */
  public PaginatedSearch byPostStatus(final String postStatus) {
    this.postStatus = PostStatus.parse(postStatus);
    return this;
  }

  /**
   * Apply filters, sorting and pagination and execute the search
   *
   * @return an {@link Uni} that will resolve into a list of {@link Post} that fulfills the search
   * requirements
   */
  public Uni<List<Post>> invoke() {
    return searchCallback.apply(this);
  }
}