package com.patricia.comunicacion;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableRabbit
public class ComunicacionApplication {

	public static void main(String[] args) {
		SpringApplication.run(ComunicacionApplication.class, args);
	}
}
