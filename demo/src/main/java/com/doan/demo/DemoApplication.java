package com.doan.demo;

import com.doan.demo.model.Admin;
import com.doan.demo.repository.AdminRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

	/**
	 * Chạy 1 lần khi khởi động.
	 * Nếu chưa có admin hoặc password không phải BCrypt → tự tạo/reset.
	 * Xóa method này sau khi đăng nhập thành công lần đầu.
	 */
	@Bean
	public CommandLineRunner initAdmin(AdminRepository adminRepository) {
		return args -> {
			BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
			String rawPassword = "admin123";
			String hash = encoder.encode(rawPassword);

			adminRepository.findByUsername("admin").ifPresentOrElse(
					admin -> {
						// Luôn reset hash bằng hash mới tạo ngay trong JVM này
						admin.setPassword(hash);
						adminRepository.save(admin);
						System.out.println("✅ [INIT] Admin password đã được reset.");
						System.out.println("✅ [INIT] Đăng nhập: admin / " + rawPassword);
						System.out.println("✅ [INIT] Hash mới: " + hash);
						System.out.println("✅ [INIT] Verify: " + encoder.matches(rawPassword, hash));
					},
					() -> {
						Admin admin = new Admin();
						admin.setUsername("admin");
						admin.setPassword(hash);
						adminRepository.save(admin);
						System.out.println("✅ [INIT] Admin mới đã được tạo: admin / " + rawPassword);
					}
			);
		};
	}
}