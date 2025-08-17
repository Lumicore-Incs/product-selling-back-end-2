package com.selling;

import com.selling.controller.DashboardController;
import com.selling.model.User;
import com.selling.repository.UserRepo;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

@SpringBootApplication
public class DemoApplication {
	private static final Logger logger = LoggerFactory.getLogger(DemoApplication.class);
	BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
	private final UserRepo userRepo;
	private final DashboardController dashboardController;

	@Autowired
	public DemoApplication(UserRepo userRepo, DashboardController dashboardController) {
		this.userRepo = userRepo;
        this.dashboardController = dashboardController;
    }

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

	@PostConstruct
	public void initUsers() {
		try {
			Optional<User> byEmail = userRepo.findByEmail("nipuna315np@gmail.com");
			if (byEmail.isEmpty()) {
				String encodePassword = passwordEncoder.encode("1234");
				userRepo.save(new User(null,"piyumal", "nipuna315np@gmail.com", "0754585756", "ADMIN", String.valueOf(LocalDateTime.now()), "active", null,  encodePassword));
			}
		} catch (Exception e) {
			logger.error("An error occurred during user initialization.", e);
		}

		dashboardController.updateOrderDetails();

		new Thread(() -> {
			while (true) {
				try {
					// මිනිත්තු 2කට වරක් print කරන්න
					Thread.sleep(30000); // 120000 ms = 2 minutes
					System.out.println("Background check - 2 minutes elapsed");
					dashboardController.updateOrderDetails();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}).start();

	}
}
