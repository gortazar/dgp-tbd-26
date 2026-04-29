package es.codeurjc.board.service;

import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import es.codeurjc.board.model.Post;
import es.codeurjc.board.repository.PostRepository;

@Service
public class SmartPostService {

	private final static Logger log = LoggerFactory.getLogger(SmartPostService.class);
	
	private PostRepository postRepository;

	public SmartPostService(PostRepository posts) {
		this.postRepository = posts;
	}

	public Page<Post> getPosts(Pageable pageRequest) {
		log.info("Using smart service...");
		return postRepository.findAll(pageRequest);
	}

    public Optional<Post> findById(long id) {
        log.info("Using smart service...");
        return postRepository.findById(id);
    }

    public void createPost(Post post) {
        log.info("Using smart service...");
        if(post.getTitle() == null || post.getTitle().isEmpty() || post.getText() == null || post.getText().isEmpty()) {
            throw new IllegalArgumentException("Invalid post");
        }
        postRepository.save(post);
    }

    public Post replacePost(Post newPost, long id) {
        log.info("Using smart service...");
        
        Post post = postRepository.findById(id).orElseThrow();

        if(newPost.getTitle() != null && !newPost.getTitle().isEmpty()) {
            post.setTitle(newPost.getTitle());
        }
        if(newPost.getText() != null && !newPost.getText().isEmpty()) {
            post.setText(newPost.getText());
        }

        if(!Objects.equals(newPost.getUsername(), post.getUsername())) {
            throw new IllegalArgumentException("Cannot change the username of a post");
        }

		postRepository.save(post);

		return post;

    }

    public Post deleteById(long id) {
        Post post = postRepository.findById(id).orElseThrow();
		postRepository.deleteById(id);
		return post;
    }
    
}
