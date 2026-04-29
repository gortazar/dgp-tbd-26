package es.codeurjc.board.service;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import es.codeurjc.board.model.Post;
import es.codeurjc.board.repository.PostRepository;

@Service
public class PostService {

	private final static Logger log = LoggerFactory.getLogger(PostService.class);
	
	private PostRepository postRepository;

	public PostService(PostRepository posts) {
		this.postRepository = posts;
		
	}

	public Page<Post> getPosts(Pageable pageRequest) {
		log.info("Usando el service...");
		return postRepository.findAll(pageRequest);
	}

	public Optional<Post> findById(long id) {
		return postRepository.findById(id);
	}

	public void createPost(Post post) {
		postRepository.save(post);		
	}

    public Post replacePost(Post newPost, long id) {
		Post post = postRepository.findById(id).orElseThrow();

		newPost.setId(id);

		// We assume that comments are not updated with PUT operation
		post.getComments().forEach(c -> newPost.addComment(c));

		postRepository.save(newPost);

		return newPost;
    }

	public Post deleteById(long id) {
		Post post = postRepository.findById(id).orElseThrow();

		postRepository.deleteById(id);

		return post;
	}

}
