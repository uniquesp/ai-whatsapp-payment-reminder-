package com.example.yoga_reminder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class YogaReminderApplication {

	public static void main(String[] args) {
		SpringApplication.run(YogaReminderApplication.class, args);
	}

}
