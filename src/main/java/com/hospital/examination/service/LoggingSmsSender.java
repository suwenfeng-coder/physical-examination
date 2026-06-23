package com.hospital.examination.service;

import com.hospital.examination.model.SmsPurpose;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.sms.provider", havingValue = "logging", matchIfMissing = true)
public class LoggingSmsSender implements SmsSender {
    private static final Logger log = LoggerFactory.getLogger(LoggingSmsSender.class);

    @Override
    public void send(String phone, String code, SmsPurpose purpose) {
        log.info("[开发短信] 手机号={}，用途={}，验证码={}，5分钟内有效", mask(phone), purpose, code);
    }

    private String mask(String phone) {
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
}
