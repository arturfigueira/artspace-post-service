package com.artspace.post;

import com.artspace.post.outgoing.PostDTO;
import org.bson.types.ObjectId;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Default mapper for {@link Post} data
 * <p>
 * This class follows map struct approach to convert and revert entities to dtos. Refer to <a
 * href="https://mapstruct.org/">its documentation</a> for more details
 */
@Mapper(componentModel = "cdi")
public interface PostMapper {

  @Mapping(source = "author", target = "authorUsername")
  PostDTO toDTO(final Post entity);

  default String toString(final ObjectId id) {
    return id == null ? null : id.toString();
  }

  default ObjectId fromString(final String id) {
    return id == null ? null : new ObjectId(id);
  }
}
