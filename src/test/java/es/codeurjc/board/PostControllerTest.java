package es.codeurjc.board;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import es.codeurjc.board.controller.PostController;
import es.codeurjc.board.model.Comment;
import es.codeurjc.board.model.Post;
import es.codeurjc.board.repository.CommentRepository;
import es.codeurjc.board.service.SmartPostService;
import io.getunleash.Unleash;

@WebMvcTest(PostController.class)
class PostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SmartPostService postService;

    @MockBean
    private CommentRepository commentRepository;

    @MockBean
    private Unleash unleash;

    // ---- GET /posts/ ----

    @Test
    void getPosts_returnsPageOfPosts() throws Exception {
        Post post = buildPost(1L, "Alice", "Title", "Text");
        Page<Post> page = new PageImpl<>(List.of(post));
        when(postService.getPosts(any())).thenReturn(page);

        mockMvc.perform(get("/posts/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id", is(1)))
                .andExpect(jsonPath("$.content[0].username", is("Alice")));
    }

    // ---- GET /posts/{id} ----

    @Test
    void getPost_returnsPost_whenFound() throws Exception {
        Post post = buildPost(1L, "Alice", "Title", "Text");
        when(postService.findById(1L)).thenReturn(Optional.of(post));

        mockMvc.perform(get("/posts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.username", is("Alice")))
                .andExpect(jsonPath("$.title", is("Title")))
                .andExpect(jsonPath("$.text", is("Text")));
    }

    @Test
    void getPost_returns404_whenNotFound() throws Exception {
        when(postService.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/posts/99"))
                .andExpect(status().isNotFound());
    }

    // ---- POST /posts/ ----

    @Test
    void createPost_returns201_withLocation_whenValid() throws Exception {
        Post post = buildPost(0L, "Alice", "Title", "Some text");
        doAnswer(invocation -> {
            Post p = invocation.getArgument(0);
            p.setId(1L);
            return null;
        }).when(postService).createPost(any(Post.class));

        mockMvc.perform(post("/posts/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(post)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/posts/1")))
                .andExpect(jsonPath("$.title", is("Title")));
    }

    @Test
    void createPost_returns400_whenTitleIsNull() throws Exception {
        Post post = buildPost(0L, "Alice", null, "Text");

        mockMvc.perform(post("/posts/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(post)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createPost_returns400_whenTitleIsEmpty() throws Exception {
        Post post = buildPost(0L, "Alice", "", "Text");

        mockMvc.perform(post("/posts/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(post)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createPost_returns400_whenTextIsNull() throws Exception {
        Post post = buildPost(0L, "Alice", "Title", null);

        mockMvc.perform(post("/posts/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(post)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createPost_returns400_whenTextIsEmpty() throws Exception {
        Post post = buildPost(0L, "Alice", "Title", "");

        mockMvc.perform(post("/posts/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(post)))
                .andExpect(status().isBadRequest());
    }

    // ---- PUT /posts/{id} ----

    @Test
    void replacePost_returnsUpdatedPost() throws Exception {
        Post updated = buildPost(1L, "Alice", "New Title", "New Text");
        when(postService.replacePost(any(Post.class), eq(1L))).thenReturn(updated);

        mockMvc.perform(put("/posts/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("New Title")))
                .andExpect(jsonPath("$.text", is("New Text")));
    }

    @Test
    void replacePost_returns404_whenNotFound() throws Exception {
        when(postService.replacePost(any(Post.class), eq(99L)))
                .thenThrow(new NoSuchElementException());

        Post post = buildPost(0L, "Alice", "Title", "Text");
        mockMvc.perform(put("/posts/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(post)))
                .andExpect(status().isNotFound());
    }

    // ---- DELETE /posts/{id} ----

    @Test
    void deletePost_returnsDeletedPost() throws Exception {
        Post post = buildPost(1L, "Alice", "Title", "Text");
        when(postService.deleteById(1L)).thenReturn(post);

        mockMvc.perform(delete("/posts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)));
    }

    @Test
    void deletePost_returns404_whenNotFound() throws Exception {
        when(postService.deleteById(99L)).thenThrow(new NoSuchElementException());

        mockMvc.perform(delete("/posts/99"))
                .andExpect(status().isNotFound());
    }

    // ---- GET /posts/{idPost}/comments/{idComment} ----

    @Test
    void getComment_returnsComment_whenFound() throws Exception {
        Post post = buildPost(1L, "Alice", "Title", "Text");
        Comment comment = buildComment(10L, "Bob", "Nice post", post);
        when(commentRepository.findById(10L)).thenReturn(Optional.of(comment));

        mockMvc.perform(get("/posts/1/comments/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(10)))
                .andExpect(jsonPath("$.username", is("Bob")))
                .andExpect(jsonPath("$.comment", is("Nice post")));
    }

    @Test
    void getComment_returns404_whenNotFound() throws Exception {
        when(commentRepository.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/posts/1/comments/99"))
                .andExpect(status().isNotFound());
    }

    // ---- POST /posts/{idPost}/comments/ ----

    @Test
    void addComment_returns201_withLocation() throws Exception {
        Post post = buildPost(1L, "Alice", "Title", "Text");
        when(postService.findById(1L)).thenReturn(Optional.of(post));

        Comment comment = buildComment(0L, "Bob", "Great!", null);
        doAnswer(invocation -> {
            Comment c = invocation.getArgument(0);
            c.setId(5L);
            return c;
        }).when(commentRepository).save(any(Comment.class));

        mockMvc.perform(post("/posts/1/comments/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(comment)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/posts/1/comments/5")))
                .andExpect(jsonPath("$.username", is("Bob")));
    }

    @Test
    void addComment_returns404_whenPostNotFound() throws Exception {
        when(postService.findById(99L)).thenReturn(Optional.empty());

        Comment comment = buildComment(0L, "Bob", "Great!", null);
        mockMvc.perform(post("/posts/99/comments/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(comment)))
                .andExpect(status().isNotFound());
    }

    // ---- PUT /posts/{idPost}/comments/{idComment} ----

    @Test
    void replaceComment_returnsUpdatedComment() throws Exception {
        Post post = buildPost(1L, "Alice", "Title", "Text");
        Comment existing = buildComment(10L, "Bob", "Old text", post);
        when(commentRepository.findById(10L)).thenReturn(Optional.of(existing));

        Comment update = buildComment(0L, "Bob", "Updated text", null);
        doAnswer(invocation -> invocation.getArgument(0))
                .when(commentRepository).save(any(Comment.class));

        mockMvc.perform(put("/posts/1/comments/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comment", is("Updated text")));
    }

    @Test
    void replaceComment_returns404_whenCommentNotFound() throws Exception {
        when(commentRepository.findById(99L)).thenReturn(Optional.empty());

        Comment update = buildComment(0L, "Bob", "Text", null);
        mockMvc.perform(put("/posts/1/comments/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isNotFound());
    }

    // ---- DELETE /posts/{idPost}/comments/{idComment} ----

    @Test
    void deleteComment_returnsDeletedComment() throws Exception {
        Post post = buildPost(1L, "Alice", "Title", "Text");
        Comment comment = buildComment(10L, "Bob", "Nice post", post);
        when(commentRepository.findById(10L)).thenReturn(Optional.of(comment));

        mockMvc.perform(delete("/posts/1/comments/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(10)));
    }

    @Test
    void deleteComment_returns404_whenCommentNotFound() throws Exception {
        when(commentRepository.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(delete("/posts/1/comments/99"))
                .andExpect(status().isNotFound());
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

    private Comment buildComment(long id, String username, String text, Post post) {
        Comment comment = new Comment();
        comment.setId(id);
        comment.setUsername(username);
        comment.setComment(text);
        comment.setPost(post);
        return comment;
    }
}
