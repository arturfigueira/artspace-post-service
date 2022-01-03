package com.artspace.post;

import io.quarkus.mongodb.panache.common.MongoEntity;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.With;
import org.bson.types.ObjectId;

/**
 * Represents a Post author.
 *
 * @since 1.0.0
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
@ToString
@Data
@MongoEntity(collection = "author")
@With(AccessLevel.PROTECTED)
public class Author {

  private ObjectId id;

  @NotNull
  @Size(min = 3, max = 50)
  private String username;

  @NotNull
  private boolean isActive;

  public void activate() {
    this.isActive = true;
  }
}
