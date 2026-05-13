package com.lumen.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class LumenApplication {

	public static void main(String[] args) {
		SpringApplication.run(LumenApplication.class, args);
		System.out.println("The Lumen server has started successfully. Visit http://localhost:8080 to access the Trace Explorer dashboard.");
	}

}
