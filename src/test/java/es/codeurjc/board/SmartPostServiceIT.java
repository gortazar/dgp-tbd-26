package es.codeurjc.board;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import es.codeurjc.board.model.Comment;
import es.codeurjc.board.model.Post;
import es.codeurjc.board.repository.CommentRepository;
import es.codeurjc.board.repository.PostRepository;
import es.codeurjc.board.service.SmartPostService;
import io.getunleash.Unleash;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class SmartPostServiceIT {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("post")
            .withUsername("root")
            .withPassword("root");

    @Autowired
    private SmartPostService postService;

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

    @Test
    void createPost_and_findById_returnsPersistedPost() {
        Post post = buildPost("Alice", "Title", "Some text");

        postService.createPost(post);

        Optional<Post> found = postService.findById(post.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("Alice");
        assertThat(found.get().getTitle()).isEqualTo("Title");
        assertThat(found.get().getText()).isEqualTo("Some text");
    }

    @Test
    void createPost_withComments_persistsCommentsAndPost() {
        Post post = buildPost("Bob", "Selling bike", "Details here");
        post.addComment(new Comment("Charlie", "Interested!"));
        post.addComment(new Comment("Diana", "How much?"));

        postService.createPost(post);

        Optional<Post> found = postService.findById(post.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getComments()).hasSize(2);
    }

    @Test
    void createPost_throwsException_whenTitleIsEmpty() {
        Post post = buildPost("Alice", "", "Text");

        assertThatThrownBy(() -> postService.createPost(post))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getPosts_returnsPaginatedResults() {
        postService.createPost(buildPost("Alice", "First", "Text 1"));
        postService.createPost(buildPost("Bob", "Second", "Text 2"));
        postService.createPost(buildPost("Carol", "Third", "Text 3"));

        Page<Post> page = postService.getPosts(PageRequest.of(0, 2));

        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent()).hasSize(2);
    }

    @Test
    void replacePost_updatesFieldsInDatabase() {
        Post post = buildPost("Alice", "Original Title", "Original Text");
        postService.createPost(post);

        Post update = buildPost("Alice", "Updated Title", "Updated Text");
        postService.replacePost(update, post.getId());

        Post found = postService.findById(post.getId()).orElseThrow();
        assertThat(found.getTitle()).isEqualTo("Updated Title");
        assertThat(found.getText()).isEqualTo("Updated Text");
    }

    @Test
    void replacePost_throwsException_whenUsernameChanges() {
        Post post = buildPost("Alice", "Title", "Text");
        postService.createPost(post);

        Post update = buildPost("Bob", "Title", "Text");
        assertThatThrownBy(() -> postService.replacePost(update, post.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot change the username");
    }

    @Test
    void replacePost_throwsException_whenPostNotFound() {
        assertThatThrownBy(() -> postService.replacePost(new Post(), 999L))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void deleteById_removesPostFromDatabase() {
        Post post = buildPost("Alice", "Title", "Text");
        postService.createPost(post);
        long id = post.getId();

        Post deleted = postService.deleteById(id);

        assertThat(deleted.getId()).isEqualTo(id);
        assertThat(postService.findById(id)).isEmpty();
    }

    @Test
    void deleteById_throwsException_whenPostNotFound() {
        assertThatThrownBy(() -> postService.deleteById(999L))
                .isInstanceOf(NoSuchElementException.class);
    }

    private Post buildPost(String username, String title, String text) {
        Post post = new Post();
        post.setUsername(username);
        post.setTitle(title);
        post.setText(text);
        return post;
    }
}
