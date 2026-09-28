package com.tapecloud.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${mail.from}")
    private String from;

    @Value("${brevo.api-key:}")
    private String brevoApiKey;

    @Value("${brevo.sender-name:TapeCloud}")
    private String brevoSenderName;

    public EmailService(JavaMailSender mailSender, ObjectMapper objectMapper) {
        this.mailSender = mailSender;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    public void sendVerificationCode(String toEmail, String code) {
        String subject = "Tu código de verificación de TapeCloud";
        String text = "Tu código de verificación es: " + code + "\n\n"
                + "Vence en 5 minutos. Si no creaste una cuenta en TapeCloud, ignorá este mensaje.";
        // Brevo por HTTPS: Railway bloquea la salida SMTP a Gmail (timeout en puerto 587).
        if (brevoApiKey != null && !brevoApiKey.isBlank()) {
            sendViaBrevo(toEmail, subject, text);
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
    }

    private void sendViaBrevo(String toEmail, String subject, String text) {
        try {
            Map<String, Object> body = Map.of(
                    "sender", Map.of("email", from, "name", brevoSenderName),
                    "to", List.of(Map.of("email", toEmail)),
                    "subject", subject,
                    "textContent", text);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.brevo.com/v3/smtp/email"))
                    .timeout(Duration.ofSeconds(15))
                    .header("accept", "application/json")
                    .header("api-key", brevoApiKey)
                    .header("content-type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.error("Brevo rechazó el email: status={} body={}", response.statusCode(), response.body());
                throw new MailSendException("Brevo rechazó el envío: " + response.statusCode());
            }
        } catch (MailSendException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new MailSendException("Fallo al enviar email por Brevo", ex);
        }
    }
}
