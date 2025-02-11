package com.umutyenidil.springwhatsapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class SpringWhatsappApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringWhatsappApplication.class, args);
	}

}
