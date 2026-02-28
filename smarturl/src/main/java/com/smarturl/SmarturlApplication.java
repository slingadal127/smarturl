package com.smarturl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SmarturlApplication {
	public static void main(String[] args) {
		SpringApplication.run(SmarturlApplication.class, args);
	}
}