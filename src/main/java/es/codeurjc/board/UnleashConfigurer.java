package es.codeurjc.board;

import java.net.URI;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.getunleash.DefaultUnleash;
import io.getunleash.Unleash;
import io.getunleash.util.UnleashConfig;

@Configuration
public class UnleashConfigurer {

	@Bean
	public Unleash unleash() {
		UnleashConfig config = UnleashConfig.builder()
		        .appName("java-posts")
		        .apiKey("default:development.unleash-insecure-api-token")
		        .unleashAPI(URI.create("http://localhost:4242/api/"))
		        .build();

		return new DefaultUnleash(config);

	}
	
}
