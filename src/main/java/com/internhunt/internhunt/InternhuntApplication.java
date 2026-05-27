package com.internhunt.internhunt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = {org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration.class})
public class InternhuntApplication {
	public static void main(String[] args) {
		SpringApplication.run(InternhuntApplication.class, args);
	}
}
