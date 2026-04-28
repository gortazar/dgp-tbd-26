package es.codeurjc.board.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import es.codeurjc.board.model.Post;
import es.codeurjc.board.repository.PostRepository;

@Service
public class PostService {
	
	private PostRepository postRepository;

	public PostService(PostRepository posts) {
		this.postRepository = posts;
		
	}

	public Page<Post> getPosts(Pageable pageRequest) {
		return postRepository.findAll(pageRequest);
	}

	
	
}
