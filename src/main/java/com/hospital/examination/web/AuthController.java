package com.hospital.examination.web;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {
    @Value("${app.admin.username:admin}")
    private String adminUsername;

    @Value("${app.admin.password:123456}")
    private String adminPassword;

    @GetMapping("/login")
    public String loginPage(HttpSession session) {
        return session.getAttribute("LOGIN_USER") == null ? "login" : "redirect:/";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username, @RequestParam String password,
                        HttpSession session, Model model) {
        if (adminUsername.equals(username) && adminPassword.equals(password)) {
            session.setAttribute("LOGIN_USER", username);
            return "redirect:/";
        }
        model.addAttribute("error", "用户名或密码错误");
        model.addAttribute("username", username);
        return "login";
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}
