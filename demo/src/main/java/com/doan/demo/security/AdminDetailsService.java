package com.doan.demo.security;

import com.doan.demo.model.Admin;
import com.doan.demo.repository.AdminRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminDetailsService implements UserDetailsService {

    @Autowired
    private AdminRepository adminRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Admin admin = adminRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Không tìm thấy admin: " + username));

        System.out.println(">>> [DEBUG] Username tìm được: " + admin.getUsername());
        System.out.println(">>> [DEBUG] Hash trong DB: " + admin.getPassword());

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        boolean match = encoder.matches("admin123", admin.getPassword());
        System.out.println(">>> [DEBUG] BCrypt.matches('admin123', hash) = " + match);

        return new User(
                admin.getUsername(),
                admin.getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
    }
}