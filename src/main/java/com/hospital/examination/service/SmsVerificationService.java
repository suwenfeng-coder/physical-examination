package com.hospital.examination.service;

import com.hospital.examination.model.SmsPurpose;
import com.hospital.examination.model.SmsVerificationCode;
import com.hospital.examination.repository.SmsVerificationCodeRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class SmsVerificationService {
    private static final SecureRandom RANDOM = new SecureRandom();

    private final SmsVerificationCodeRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final SmsSender smsSender;

    @Value("${app.sms.dev-mode:true}")
    private boolean devMode;

    public SmsVerificationService(SmsVerificationCodeRepository repository,
                                  PasswordEncoder passwordEncoder,
                                  SmsSender smsSender) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.smsSender = smsSender;
    }

    @Transactional
    public String send(String phone, SmsPurpose purpose) {
        validatePhone(phone);
        repository.findTopByPhoneAndPurposeAndUsedFalseOrderByCreatedAtDesc(phone, purpose)
                .filter(existing -> Duration.between(existing.getCreatedAt(), LocalDateTime.now()).getSeconds() < 60)
                .ifPresent(existing -> {
                    throw new IllegalStateException("验证码发送过于频繁，请稍后再试");
                });

        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        SmsVerificationCode verificationCode = new SmsVerificationCode();
        verificationCode.setPhone(phone);
        verificationCode.setPurpose(purpose);
        verificationCode.setCodeHash(passwordEncoder.encode(code));
        verificationCode.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        repository.save(verificationCode);
        smsSender.send(phone, code, purpose);
        return devMode ? code : null;
    }

    @Transactional
    public void verifyAndConsume(String phone, String code, SmsPurpose purpose) {
        SmsVerificationCode verificationCode = repository
                .findTopByPhoneAndPurposeAndUsedFalseOrderByCreatedAtDesc(phone, purpose)
                .orElseThrow(() -> new IllegalArgumentException("请先获取短信验证码"));
        if (verificationCode.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("验证码已过期，请重新获取");
        }
        if (!passwordEncoder.matches(code, verificationCode.getCodeHash())) {
            throw new IllegalArgumentException("短信验证码错误");
        }
        verificationCode.setUsed(true);
    }

    public static void validatePhone(String phone) {
        if (phone == null || !phone.matches("^1\\d{10}$")) {
            throw new IllegalArgumentException("请输入正确的11位手机号码");
        }
    }
}
