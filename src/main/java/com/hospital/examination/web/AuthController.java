package com.hospital.examination.web;

import com.hospital.examination.model.SmsPurpose;
import com.hospital.examination.model.UserAccount;
import com.hospital.examination.repository.UserAccountRepository;
import com.hospital.examination.service.SmsVerificationService;
import com.hospital.examination.service.UserAuthService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.Map;

@Controller
public class AuthController {
    private final UserAuthService userAuthService;
    private final SmsVerificationService smsVerificationService;
    private final UserAccountRepository userRepository;

    public AuthController(UserAuthService userAuthService,
                          SmsVerificationService smsVerificationService,
                          UserAccountRepository userRepository) {
        this.userAuthService = userAuthService;
        this.smsVerificationService = smsVerificationService;
        this.userRepository = userRepository;
    }

    @GetMapping("/login")
    public String loginPage(HttpSession session,
                            @RequestParam(defaultValue = "password") String mode,
                            Model model) {
        if (session.getAttribute("LOGIN_USER") != null) {
            return "redirect:/";
        }
        model.addAttribute("mode", mode);
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam(defaultValue = "password") String mode,
                        @RequestParam(required = false) String identifier,
                        @RequestParam(required = false) String password,
                        @RequestParam(required = false) String phone,
                        @RequestParam(required = false) String verificationCode,
                        HttpSession session, Model model) {
        try {
            UserAccount user = "sms".equals(mode)
                    ? userAuthService.loginBySms(phone, verificationCode)
                    : userAuthService.loginByPassword(identifier, password);
            establishSession(session, user);
            return "redirect:/";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("mode", mode);
            model.addAttribute("identifier", identifier);
            model.addAttribute("phone", phone);
            return "login";
        }
    }

    @GetMapping("/register")
    public String registerPage(HttpSession session) {
        return session.getAttribute("LOGIN_USER") == null ? "register" : "redirect:/";
    }

    @PostMapping("/register")
    public String register(@RequestParam String username,
                           @RequestParam String phone,
                           @RequestParam String password,
                           @RequestParam String confirmPassword,
                           @RequestParam String verificationCode,
                           RedirectAttributes redirectAttributes,
                           Model model) {
        try {
            userAuthService.register(username, phone, password, confirmPassword, verificationCode);
            redirectAttributes.addFlashAttribute("success", "注册成功，请使用新账户登录");
            return "redirect:/login";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("username", username);
            model.addAttribute("phone", phone);
            return "register";
        }
    }

    @PostMapping("/auth/sms/send")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> sendSms(@RequestParam String phone,
                                                        @RequestParam SmsPurpose purpose) {
        Map<String, Object> response = new LinkedHashMap<>();
        try {
            if (purpose == SmsPurpose.REGISTER && userRepository.existsByPhone(phone)) {
                throw new IllegalArgumentException("该手机号已注册");
            }
            if (purpose == SmsPurpose.LOGIN && !userRepository.existsByPhone(phone)) {
                throw new IllegalArgumentException("该手机号尚未注册");
            }
            String devCode = smsVerificationService.send(phone, purpose);
            response.put("success", true);
            response.put("message", "验证码已发送，5分钟内有效");
            if (devCode != null) {
                response.put("devCode", devCode);
            }
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            response.put("success", false);
            response.put("message", ex.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    private void establishSession(HttpSession session, UserAccount user) {
        session.setAttribute("LOGIN_USER", user.getUsername());
        session.setAttribute("LOGIN_USER_ID", user.getId());
        session.setAttribute("LOGIN_ROLE", user.getRole());
        session.setMaxInactiveInterval(30 * 60);
    }
}
