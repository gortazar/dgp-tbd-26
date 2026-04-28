package es.codeurjc.board.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import es.codeurjc.board.model.Post;

public interface PostRepository extends JpaRepository<Post, Long> {

	Page<Post> findAll(Pageable pageable);
}
