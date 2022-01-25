package com.artspace.post.outgoing;

import java.time.Instant;
import java.util.Objects;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Transfer Object used to ship user data to external services
 */
@Data
@NoArgsConstructor
@ToString
public class PostDTO {

  private String id;
  private String message;
  private Instant creationTime;
  private String authorUsername;
  private boolean isEnabled;
  private Action action;


  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    PostDTO postDTO = (PostDTO) obj;
    return Objects.equals(id, postDTO.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
