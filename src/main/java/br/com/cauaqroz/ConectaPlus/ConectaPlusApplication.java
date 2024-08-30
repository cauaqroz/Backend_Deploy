package br.com.cauaqroz.ConectaPlus;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableMongoAuditing;


@SpringBootApplication
@EnableMongoAuditing
public class ConectaPlusApplication {

	public static void main(String[] args) {
		SpringApplication.run(ConectaPlusApplication.class, args);
	}

}
