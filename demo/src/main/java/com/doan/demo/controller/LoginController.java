package com.doan.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Điều hướng trang đăng nhập Admin.
 * Spring Security tự xử lý POST /login — controller này chỉ phục vụ GET.
 */
@Controller
public class LoginController {

    @GetMapping("/login")
    public String loginPage() {
        return "login";   // → templates/login.html
    }
}