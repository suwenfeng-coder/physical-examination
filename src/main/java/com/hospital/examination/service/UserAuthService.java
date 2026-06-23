package com.hospital.examination.service;

import com.hospital.examination.model.SmsPurpose;
import com.hospital.examination.model.UserAccount;
import com.hospital.examination.repository.UserAccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserAuthService {
    private final UserAccountRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SmsVerificationService smsVerificationService;

    public UserAuthService(UserAccountRepository userRepository, PasswordEncoder passwordEncoder,
                           SmsVerificationService smsVerificationService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.smsVerificationService = smsVerificationService;
    }

    @Transactional
    public UserAccount register(String username, String phone, String password,
                                String confirmPassword, String verificationCode) {
        username = username == null ? "" : username.trim();
        if (!username.matches("^[\\p{L}\\d_]{2,20}$")) {
            throw new IllegalArgumentException("用户名需为2-20位中文、字母、数字或下划线");
        }
        SmsVerificationService.validatePhone(phone);
        validatePassword(password);
        if (!password.equals(confirmPassword)) {
            throw new IllegalArgumentException("两次输入的密码不一致");
        }
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("用户名已被使用");
        }
        if (userRepository.existsByPhone(phone)) {
            throw new IllegalArgumentException("该手机号已注册");
        }

        smsVerificationService.verifyAndConsume(phone, verificationCode, SmsPurpose.REGISTER);
        UserAccount user = new UserAccount();
        user.setUsername(username);
        user.setPhone(phone);
        user.setPasswordHash(passwordEncoder.encode(password));
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public UserAccount loginByPassword(String identifier, String password) {
        UserAccount user = userRepository.findByUsernameOrPhone(identifier, identifier)
                .orElseThrow(() -> new IllegalArgumentException("用户名、手机号或密码错误"));
        if (!user.isEnabled() || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("用户名、手机号或密码错误");
        }
        return user;
    }

    @Transactional
    public UserAccount loginBySms(String phone, String verificationCode) {
        UserAccount user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new IllegalArgumentException("该手机号尚未注册"));
        if (!user.isEnabled()) {
            throw new IllegalArgumentException("该账户已停用");
        }
        smsVerificationService.verifyAndConsume(phone, verificationCode, SmsPurpose.LOGIN);
        return user;
    }

    private void validatePassword(String password) {
        if (password == null || !password.matches("^(?=.*[A-Za-z])(?=.*\\d).{8,32}$")) {
            throw new IllegalArgumentException("密码需为8-32位，并同时包含字母和数字");
        }
    }
}
