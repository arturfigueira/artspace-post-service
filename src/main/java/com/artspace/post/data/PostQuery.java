package com.artspace.post.data;

import com.artspace.post.Post;
import io.smallrye.mutiny.Uni;
import java.util.List;

interface PostQuery {

  Uni<List<Post>> invoke(final PaginatedSearch paginatedSearch);
}