package com.railbook;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
@SpringBootApplication
@EnableScheduling
public class RailBookApplication {

	public static void main(String[] args) {
		SpringApplication.run(
				RailBookApplication.class, args
		);
	}
}