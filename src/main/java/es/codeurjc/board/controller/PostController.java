package es.codeurjc.board.controller;

import static org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentRequest;

import java.net.URI;

import jakarta.annotation.PostConstruct;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import es.codeurjc.board.model.Comment;
import es.codeurjc.board.model.Post;
import es.codeurjc.board.repository.CommentRepository;
import es.codeurjc.board.service.SmartPostService;
import io.getunleash.Unleash;

@RestController
@RequestMapping("/posts")
public class PostController {

	private CommentRepository comments;

	private Unleash unleash;

	private SmartPostService postService;

	public PostController(CommentRepository comments, Unleash unleash, SmartPostService postService) {
		this.comments = comments;
		this.unleash = unleash;
		this.postService = postService;
	}
	
	@PostConstruct
	public void init() {

		if(unleash.isEnabled("demo-data-flag")) {
			Post p = new Post();
			p.setUsername("Pepe");
			p.setTitle("Vendo moto");
			p.setText("Bla bla bla...");
			p.addComment(new Comment("Juan", "Pues si"));
			p.addComment(new Comment("Maria", "Pues no"));

			postService.createPost(p);
		}
		
	}

	@GetMapping("/")
	public Page<Post> getPosts(@RequestParam(required = false) Pageable pageRequest) {
		
		if(pageRequest == null) {
			pageRequest = PageRequest.of(0, 100);
		}

		return postService.getPosts(pageRequest);
	}

	@GetMapping("/{id}")
	public Post getPost(@PathVariable long id) {

		return postService.findById(id).orElseThrow();
	}

	@PostMapping("/")
	public ResponseEntity<Post> createPost(@RequestBody Post post) {

		if(post.getTitle() == null || post.getTitle().isEmpty() || post.getText() == null || post.getText().isEmpty()) {
			return ResponseEntity.badRequest().build();
		}
		
		postService.createPost(post);

		URI location = fromCurrentRequest().path("/{id}").buildAndExpand(post.getId()).toUri();

		return ResponseEntity.created(location).body(post);
	}

	@PutMapping("/{id}")
	public Post replacePost(@RequestBody Post newPost, @PathVariable long id) {

		return postService.replacePost(newPost, id);
	}

	@DeleteMapping("/{id}")
	public Post deletePost(@PathVariable long id) {

		return postService.deleteById(id);
	}

	@GetMapping("/{idPost}/comments/{idComment}")
	public Comment getComment(@PathVariable long idPost, @PathVariable long idComment) {

		return comments.findById(idComment).orElseThrow();
	}

	@PostMapping("/{idPost}/comments/")
	public ResponseEntity<Comment> addComment(@PathVariable long idPost, @RequestBody Comment comment) {

		Post post = postService.findById(idPost).orElseThrow();

		comment.setPost(post);
		comments.save(comment);

		URI location = fromCurrentRequest().path("/{id}").buildAndExpand(comment.getId()).toUri();

		return ResponseEntity.created(location).body(comment);
	}

	@PutMapping("/{idPost}/comments/{idComment}")
	public Comment replaceComment(@PathVariable long idPost, @PathVariable long idComment,
			@RequestBody Comment updatedComment) {

		Comment comment = comments.findById(idComment).orElseThrow();

		updatedComment.setId(idComment);
		updatedComment.setPost(comment.getPost());
		
		comments.save(updatedComment);

		return updatedComment;
	}

	@DeleteMapping("/{idPost}/comments/{idComment}")
	public Comment deleteComment(@PathVariable long idPost, @PathVariable long idComment) {

		Comment comment = comments.findById(idComment).orElseThrow();

		comments.delete(comment);

		return comment;
	}

}
