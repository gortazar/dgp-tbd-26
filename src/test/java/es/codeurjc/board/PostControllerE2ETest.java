package es.codeurjc.board;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import es.codeurjc.board.model.Comment;
import es.codeurjc.board.model.Post;
import es.codeurjc.board.repository.CommentRepository;
import es.codeurjc.board.repository.PostRepository;
import io.getunleash.Unleash;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
class PostControllerE2ETest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("post")
            .withUsername("root")
            .withPassword("root");

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private CommentRepository commentRepository;

    @MockBean
    private Unleash unleash;

    @BeforeEach
    void cleanUp() {
        commentRepository.deleteAll();
        postRepository.deleteAll();
    }

    // ---- POST /posts/ ----

    @Test
    void createPost_returns201_withLocationHeader() {
        Post post = buildPost("Alice", "Title", "Some text");

        ResponseEntity<Post> response = restTemplate.postForEntity("/posts/", post, Post.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getHeaders().getLocation()).isNotNull();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isPositive();
        assertThat(response.getBody().getTitle()).isEqualTo("Title");
    }

    @Test
    void createPost_returns400_whenTitleIsEmpty() {
        Post post = buildPost("Alice", "", "Text");

        ResponseEntity<Void> response = restTemplate.postForEntity("/posts/", post, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void createPost_returns400_whenTextIsNull() {
        Post post = buildPost("Alice", "Title", null);

        ResponseEntity<Void> response = restTemplate.postForEntity("/posts/", post, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ---- GET /posts/ ----

    @Test
    void getPosts_returnsAllPosts() {
        restTemplate.postForEntity("/posts/", buildPost("Alice", "Post 1", "Text 1"), Post.class);
        restTemplate.postForEntity("/posts/", buildPost("Bob", "Post 2", "Text 2"), Post.class);

        ResponseEntity<String> response = restTemplate.getForEntity("/posts/", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Post 1", "Post 2");
    }

    // ---- GET /posts/{id} ----

    @Test
    void getPost_returnsPost_whenFound() {
        ResponseEntity<Post> created = restTemplate.postForEntity("/posts/", buildPost("Alice", "Title", "Text"), Post.class);
        long id = created.getBody().getId();

        ResponseEntity<Post> response = restTemplate.getForEntity("/posts/" + id, Post.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getId()).isEqualTo(id);
        assertThat(response.getBody().getTitle()).isEqualTo("Title");
    }

    @Test
    void getPost_returns404_whenNotFound() {
        ResponseEntity<Void> response = restTemplate.getForEntity("/posts/99999", Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ---- PUT /posts/{id} ----

    @Test
    void replacePost_updatesAndReturnsPost() {
        ResponseEntity<Post> created = restTemplate.postForEntity("/posts/", buildPost("Alice", "Original", "Original text"), Post.class);
        long id = created.getBody().getId();

        Post update = buildPost("Alice", "Updated Title", "Updated text");
        ResponseEntity<Post> response = restTemplate.exchange(
                "/posts/" + id, HttpMethod.PUT, new HttpEntity<>(update), Post.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getTitle()).isEqualTo("Updated Title");
        assertThat(response.getBody().getText()).isEqualTo("Updated text");
    }

    @Test
    void replacePost_returns404_whenNotFound() {
        Post update = buildPost("Alice", "Title", "Text");
        ResponseEntity<Void> response = restTemplate.exchange(
                "/posts/99999", HttpMethod.PUT, new HttpEntity<>(update), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ---- DELETE /posts/{id} ----

    @Test
    void deletePost_removesPost_andReturnsIt() {
        ResponseEntity<Post> created = restTemplate.postForEntity("/posts/", buildPost("Alice", "Title", "Text"), Post.class);
        long id = created.getBody().getId();

        ResponseEntity<Post> response = restTemplate.exchange(
                "/posts/" + id, HttpMethod.DELETE, null, Post.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getId()).isEqualTo(id);

        ResponseEntity<Void> getResponse = restTemplate.getForEntity("/posts/" + id, Void.class);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void deletePost_returns404_whenNotFound() {
        ResponseEntity<Void> response = restTemplate.exchange(
                "/posts/99999", HttpMethod.DELETE, null, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ---- POST /posts/{idPost}/comments/ ----

    @Test
    void addComment_returns201_withLocation() {
        ResponseEntity<Post> created = restTemplate.postForEntity("/posts/", buildPost("Alice", "Title", "Text"), Post.class);
        long postId = created.getBody().getId();

        Comment comment = buildComment("Bob", "Great post!");
        ResponseEntity<Comment> response = restTemplate.postForEntity(
                "/posts/" + postId + "/comments/", comment, Comment.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getHeaders().getLocation()).isNotNull();
        assertThat(response.getBody().getId()).isPositive();
        assertThat(response.getBody().getUsername()).isEqualTo("Bob");
    }

    @Test
    void addComment_returns404_whenPostNotFound() {
        Comment comment = buildComment("Bob", "Great post!");
        ResponseEntity<Void> response = restTemplate.postForEntity(
                "/posts/99999/comments/", comment, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ---- GET /posts/{idPost}/comments/{idComment} ----

    @Test
    void getComment_returnsComment_whenFound() {
        ResponseEntity<Post> createdPost = restTemplate.postForEntity("/posts/", buildPost("Alice", "Title", "Text"), Post.class);
        long postId = createdPost.getBody().getId();

        ResponseEntity<Comment> createdComment = restTemplate.postForEntity(
                "/posts/" + postId + "/comments/", buildComment("Bob", "Hello"), Comment.class);
        long commentId = createdComment.getBody().getId();

        ResponseEntity<Comment> response = restTemplate.getForEntity(
                "/posts/" + postId + "/comments/" + commentId, Comment.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getComment()).isEqualTo("Hello");
    }

    @Test
    void getComment_returns404_whenNotFound() {
        ResponseEntity<Post> createdPost = restTemplate.postForEntity("/posts/", buildPost("Alice", "Title", "Text"), Post.class);
        long postId = createdPost.getBody().getId();

        ResponseEntity<Void> response = restTemplate.getForEntity(
                "/posts/" + postId + "/comments/99999", Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ---- PUT /posts/{idPost}/comments/{idComment} ----

    @Test
    void replaceComment_updatesAndReturnsComment() {
        ResponseEntity<Post> createdPost = restTemplate.postForEntity("/posts/", buildPost("Alice", "Title", "Text"), Post.class);
        long postId = createdPost.getBody().getId();

        ResponseEntity<Comment> createdComment = restTemplate.postForEntity(
                "/posts/" + postId + "/comments/", buildComment("Bob", "Original comment"), Comment.class);
        long commentId = createdComment.getBody().getId();

        Comment update = buildComment("Bob", "Updated comment");
        ResponseEntity<Comment> response = restTemplate.exchange(
                "/posts/" + postId + "/comments/" + commentId,
                HttpMethod.PUT, new HttpEntity<>(update), Comment.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getComment()).isEqualTo("Updated comment");
    }

    // ---- DELETE /posts/{idPost}/comments/{idComment} ----

    @Test
    void deleteComment_removesComment_andReturnsIt() {
        ResponseEntity<Post> createdPost = restTemplate.postForEntity("/posts/", buildPost("Alice", "Title", "Text"), Post.class);
        long postId = createdPost.getBody().getId();

        ResponseEntity<Comment> createdComment = restTemplate.postForEntity(
                "/posts/" + postId + "/comments/", buildComment("Bob", "A comment"), Comment.class);
        long commentId = createdComment.getBody().getId();

        ResponseEntity<Comment> response = restTemplate.exchange(
                "/posts/" + postId + "/comments/" + commentId,
                HttpMethod.DELETE, null, Comment.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getId()).isEqualTo(commentId);

        ResponseEntity<Void> getResponse = restTemplate.getForEntity(
                "/posts/" + postId + "/comments/" + commentId, Void.class);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ---- helpers ----

    private Post buildPost(String username, String title, String text) {
        Post post = new Post();
        post.setUsername(username);
        post.setTitle(title);
        post.setText(text);
        return post;
    }

    private Comment buildComment(String username, String text) {
        Comment comment = new Comment();
        comment.setUsername(username);
        comment.setComment(text);
        return comment;
    }
}
