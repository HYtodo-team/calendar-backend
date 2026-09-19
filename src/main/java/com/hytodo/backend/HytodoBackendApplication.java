package com.hytodo.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@EnableJpaAuditing
@SpringBootApplication
public class HytodoBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(HytodoBackendApplication.class, args);
	}
}

