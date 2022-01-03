package com.artspace.post;

import io.quarkus.panache.common.Sort;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/**
 * Helper class to provide an easier way to filter posts by author, to paginate and sort the
 * results
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@ToString
class PostQuery {

  private static final Sort DEFAULT_SORT = Sort.by("creationTime");

  @Getter
  private final String author;

  private final PostStatus postStatus;

  private final Sort sortedBy = DEFAULT_SORT;

  @Getter
  private final Page page;

  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder companion class. Facilitates the building new instances of {@link PostQuery}
   * <p>
   * If not set, builder will assume {@link #FIRST_100SIZE} values for {@link Page} and {@link
   * PostStatus#ALL} for {@link PostStatus}.
   */
  @NoArgsConstructor
  public static class Builder {

    private static final Page FIRST_100SIZE = new Page(0, 100);

    private String author;
    private PostStatus postStatus = PostStatus.ALL;
    private Page page = FIRST_100SIZE;

    public Builder author(String author) {
      this.author = author.toLowerCase().trim();
      return this;
    }

    public Builder postStatus(String status) {
      this.postStatus = PostStatus.parse(status);
      return this;
    }

    public Builder page(int index, int size) {
      this.page = new Page(index, size);
      return this;
    }

    public PostQuery build() {
      return new PostQuery(this.author, this.postStatus, this.page);
    }
  }


  /**
   * Companion class to provide pagination to posts' search, where the amount of data involved is
   * too big to be returned without any kind of limit or filtering.
   */
  @ToString
  @Getter
  static class Page {

    private final int index;
    private final int size;

    Page(int index, int size) {
      if (index < 0 || size <= 0) {
        throw new IllegalArgumentException("Invalid page index and/or size");
      }
      this.index = index;
      this.size = size;
    }
  }

  /**
   * Companion class which provides post status filtering.
   */
  @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
  enum PostStatus {
    ENABLED(Boolean.TRUE),
    DISABLED(Boolean.FALSE),
    ALL(null);

    private final Boolean value;

    /**
     * Parse given value into a {@link PostStatus}. This is done by post status name. Parsing is
     * case-insensitive.
     *
     * @param value name of a post status
     * @return a PostStatus instance
     * @throws IllegalArgumentException if no status can be found with given value
     */
    public static PostStatus parse(String value) {
      return Arrays.stream(values())
          .filter(status -> status.name().equalsIgnoreCase(value))
          .findFirst()
          .orElseThrow(() -> new IllegalArgumentException(
              "Specified value could not be parsed into a Status"));
    }

    Optional<Boolean> getValue() {
      return Optional.ofNullable(this.value);
    }

    String buildQuery() {
      return getValue().map(aBoolean -> " and enabled=?2").orElse("");
    }
  }


  public String getQuery() {
    return "username=?1" + this.postStatus.buildQuery();
  }

  public Sort getSort() {
    return this.sortedBy;
  }

  public Object[] getParameters() {
    final var params = new ArrayList<>();

    params.add(author);

    this.postStatus.getValue().ifPresent(params::add);

    return params.toArray();
  }
}
