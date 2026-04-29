package es.codeurjc.board.service;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import es.codeurjc.board.model.Post;

public interface PostServiceFacade {

    Page<Post> getPosts(Pageable pageRequest);

    Optional<Post> findById(long id);

    void createPost(Post post);

    Post replacePost(Post newPost, long id);

    Post deleteById(long id);

}