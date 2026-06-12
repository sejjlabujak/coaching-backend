package com.coaching_app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CoachingAppApplication {

//	public static void main(String[] args) {
//		SpringApplication.run(CoachingAppApplication.class, args);
//	}

	public static void main(String[] args) {
		System.out.println("HASH: " + new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("admin123"));
		SpringApplication.run(CoachingAppApplication.class, args);
	}
}
