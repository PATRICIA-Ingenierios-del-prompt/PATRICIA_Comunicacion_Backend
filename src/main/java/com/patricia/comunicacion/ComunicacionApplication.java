package com.patricia.comunicacion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
public class ComunicacionApplication {

	public static void main(String[] args) {
		SpringApplication.run(ComunicacionApplication.class, args);
	}
}
