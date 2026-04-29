package es.codeurjc.board.service;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import es.codeurjc.board.model.Post;

public class SmartPostService implements PostServiceFacade {

    @Override
    public Page<Post> getPosts(Pageable pageRequest) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getPosts'");
    }

    @Override
    public Optional<Post> findById(long id) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'findById'");
    }

    @Override
    public void createPost(Post post) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'createPost'");
    }

    @Override
    public Post replacePost(Post newPost, long id) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'replacePost'");
    }

    @Override
    public Post deleteById(long id) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'deleteById'");
    }
    
}
