package com.tapecloud.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${mail.from}")
    private String from;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendVerificationCode(String toEmail, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(toEmail);
        message.setSubject("Tu código de verificación de TapeCloud");
        message.setText(
                "Tu código de verificación es: " + code + "\n\n"
                        + "Vence en 5 minutos. Si no creaste una cuenta en TapeCloud, ignorá este mensaje."
        );
        mailSender.send(message);
    }
}
