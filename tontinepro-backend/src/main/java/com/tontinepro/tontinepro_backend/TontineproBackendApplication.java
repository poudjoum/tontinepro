package com.tontinepro.tontinepro_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TontineproBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(TontineproBackendApplication.class, args);
	}

}
