package com.artspace.post.data;

import java.util.Arrays;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

/**
 * Helper class to provide post status filtering.
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
  static PostStatus parse(String value) {
    return Arrays.stream(values())
        .filter(status -> status.name().equalsIgnoreCase(value))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException(
            "Specified value could not be parsed into a Status"));
  }

  Optional<Boolean> getValue() {
    return Optional.ofNullable(this.value);
  }
}