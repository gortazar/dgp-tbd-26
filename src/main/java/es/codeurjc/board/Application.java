package es.codeurjc.board;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import es.codeurjc.board.repository.PostRepository;
import es.codeurjc.board.service.PostService;
import es.codeurjc.board.service.PostServiceFacade;
import es.codeurjc.board.service.SmartPostService;
import io.getunleash.Unleash;

@SpringBootApplication
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@Bean
	public PostServiceFacade postService(PostRepository posts, Unleash unleash) {
		if(unleash.isEnabled("smart-service-flag")) {
			return new SmartPostService(posts);
		} else {
			return new PostService(posts);	
		}
	}

}
