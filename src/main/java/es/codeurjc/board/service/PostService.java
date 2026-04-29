package es.codeurjc.board.service;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import es.codeurjc.board.model.Post;
import es.codeurjc.board.repository.PostRepository;

public class PostService implements PostServiceFacade {

	private final static Logger log = LoggerFactory.getLogger(PostService.class);
	
	private PostRepository postRepository;

	public PostService(PostRepository posts) {
		this.postRepository = posts;
		
	}

	@Override
	public Page<Post> getPosts(Pageable pageRequest) {
		log.info("Usando el service...");
		return postRepository.findAll(pageRequest);
	}

	@Override
	public Optional<Post> findById(long id) {
		return postRepository.findById(id);
	}

	@Override
	public void createPost(Post post) {
		postRepository.save(post);		
	}

    @Override
	public Post replacePost(Post newPost, long id) {
		Post post = postRepository.findById(id).orElseThrow();

		newPost.setId(id);

		// We assume that comments are not updated with PUT operation
		post.getComments().forEach(c -> newPost.addComment(c));

		postRepository.save(newPost);

		return newPost;
    }

	@Override
	public Post deleteById(long id) {
		Post post = postRepository.findById(id).orElseThrow();

		postRepository.deleteById(id);

		return post;
	}

}
