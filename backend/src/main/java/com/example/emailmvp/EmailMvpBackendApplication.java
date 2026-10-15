package com.example.emailmvp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class EmailMvpBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(EmailMvpBackendApplication.class, args);
	}

}
