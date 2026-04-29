package es.codeurjc.board;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import es.codeurjc.board.model.Post;
import es.codeurjc.board.repository.PostRepository;
import es.codeurjc.board.service.SmartPostService;

@ExtendWith(MockitoExtension.class)
class SmartPostServiceTest {

    @Mock
    private PostRepository postRepository;

    @InjectMocks
    private SmartPostService postService;

    // ---- getPosts ----

    @Test
    void getPosts_delegatesToRepository() {
        Post post = buildPost(1L, "Alice", "Title", "Text");
        PageRequest pageRequest = PageRequest.of(0, 10);
        Page<Post> expected = new PageImpl<>(List.of(post));
        when(postRepository.findAll(pageRequest)).thenReturn(expected);

        Page<Post> result = postService.getPosts(pageRequest);

        assertThat(result).isEqualTo(expected);
        verify(postRepository).findAll(pageRequest);
    }

    // ---- findById ----

    @Test
    void findById_returnsPost_whenFound() {
        Post post = buildPost(1L, "Alice", "Title", "Text");
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));

        Optional<Post> result = postService.findById(1L);

        assertThat(result).contains(post);
    }

    @Test
    void findById_returnsEmpty_whenNotFound() {
        when(postRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<Post> result = postService.findById(99L);

        assertThat(result).isEmpty();
    }

    // ---- createPost ----

    @Test
    void createPost_savesPost_whenValid() {
        Post post = buildPost(0L, "Alice", "Title", "Some text");

        postService.createPost(post);

        verify(postRepository).save(post);
    }

    @Test
    void createPost_throwsException_whenTitleIsNull() {
        Post post = buildPost(0L, "Alice", null, "Text");

        assertThatThrownBy(() -> postService.createPost(post))
                .isInstanceOf(IllegalArgumentException.class);
        verify(postRepository, never()).save(any());
    }

    @Test
    void createPost_throwsException_whenTitleIsEmpty() {
        Post post = buildPost(0L, "Alice", "", "Text");

        assertThatThrownBy(() -> postService.createPost(post))
                .isInstanceOf(IllegalArgumentException.class);
        verify(postRepository, never()).save(any());
    }

    @Test
    void createPost_throwsException_whenTextIsNull() {
        Post post = buildPost(0L, "Alice", "Title", null);

        assertThatThrownBy(() -> postService.createPost(post))
                .isInstanceOf(IllegalArgumentException.class);
        verify(postRepository, never()).save(any());
    }

    @Test
    void createPost_throwsException_whenTextIsEmpty() {
        Post post = buildPost(0L, "Alice", "Title", "");

        assertThatThrownBy(() -> postService.createPost(post))
                .isInstanceOf(IllegalArgumentException.class);
        verify(postRepository, never()).save(any());
    }

    // ---- replacePost ----

    @Test
    void replacePost_updatesTitle_whenTitleProvided() {
        Post stored = buildPost(1L, "Alice", "Old Title", "Old Text");
        Post update = buildPost(0L, "Alice", "New Title", null);
        when(postRepository.findById(1L)).thenReturn(Optional.of(stored));

        Post result = postService.replacePost(update, 1L);

        assertThat(result.getTitle()).isEqualTo("New Title");
        assertThat(result.getText()).isEqualTo("Old Text");
        verify(postRepository).save(stored);
    }

    @Test
    void replacePost_updatesText_whenTextProvided() {
        Post stored = buildPost(1L, "Alice", "Old Title", "Old Text");
        Post update = buildPost(0L, "Alice", null, "New Text");
        when(postRepository.findById(1L)).thenReturn(Optional.of(stored));

        Post result = postService.replacePost(update, 1L);

        assertThat(result.getText()).isEqualTo("New Text");
        assertThat(result.getTitle()).isEqualTo("Old Title");
        verify(postRepository).save(stored);
    }

    @Test
    void replacePost_throwsException_whenUsernameChanges() {
        Post stored = buildPost(1L, "Alice", "Title", "Text");
        Post update = buildPost(0L, "Bob", "New Title", "New Text");
        when(postRepository.findById(1L)).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> postService.replacePost(update, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot change the username");
        verify(postRepository, never()).save(any());
    }

    @Test
    void replacePost_throwsException_whenPostNotFound() {
        when(postRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.replacePost(new Post(), 99L))
                .isInstanceOf(NoSuchElementException.class);
    }

    // ---- deleteById ----

    @Test
    void deleteById_deletesAndReturnsPost() {
        Post post = buildPost(1L, "Alice", "Title", "Text");
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));

        Post result = postService.deleteById(1L);

        assertThat(result).isEqualTo(post);
        verify(postRepository).deleteById(1L);
    }

    @Test
    void deleteById_throwsException_whenPostNotFound() {
        when(postRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.deleteById(99L))
                .isInstanceOf(NoSuchElementException.class);
        verify(postRepository, never()).deleteById(any());
    }

    // ---- helpers ----

    private Post buildPost(long id, String username, String title, String text) {
        Post post = new Post();
        post.setId(id);
        post.setUsername(username);
        post.setTitle(title);
        post.setText(text);
        return post;
    }
}
