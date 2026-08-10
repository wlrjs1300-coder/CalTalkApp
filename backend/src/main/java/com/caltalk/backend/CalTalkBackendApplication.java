package com.caltalk.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CalTalkBackendApplication {

	public static void main(String[] args) {
		RenderEnvironmentNormalizer.normalize();
		SpringApplication.run(CalTalkBackendApplication.class, args);
	}

}
