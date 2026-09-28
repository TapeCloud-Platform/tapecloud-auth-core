package com.tapecloud.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.internet.MimeMessage;
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
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
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
        String html = verificationHtml(code);
        // Brevo por HTTPS: Railway bloquea la salida SMTP a Gmail (timeout en puerto 587).
        if (brevoApiKey != null && !brevoApiKey.isBlank()) {
            sendViaBrevo(toEmail, subject, text, html);
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(from);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(text, html);
            mailSender.send(message);
        } catch (Exception ex) {
            throw new MailSendException("Fallo al enviar email de verificación", ex);
        }
    }

    /** Plantilla oscura compatible con clientes de email (tablas + estilos inline). */
    private String verificationHtml(String code) {
        StringBuilder digits = new StringBuilder();
        for (char c : code.toCharArray()) {
            digits.append("<td style=\"background:#1a2236;border:1px solid #2b3458;border-radius:12px;")
                    .append("font-family:monospace,monospace;font-size:28px;font-weight:bold;color:#ffffff;")
                    .append("width:52px;height:60px;text-align:center;vertical-align:middle;\">")
                    .append(c).append("</td><td style=\"width:6px;\"></td>");
        }
        return "<!doctype html><html><body style=\"margin:0;padding:0;background-color:#070a12;\">"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\">"
                + "<tr><td align=\"center\" style=\"padding:32px 16px;\">"
                + "<table role=\"presentation\" width=\"480\" cellpadding=\"0\" cellspacing=\"0\" "
                + "style=\"max-width:480px;background-color:#0d1322;border:1px solid #232c47;border-radius:20px;overflow:hidden;\">"
                + "<tr><td style=\"background:linear-gradient(135deg,#d3203e,#3b82f6);padding:22px 28px;\">"
                + "<div style=\"font-family:Arial,sans-serif;font-size:20px;font-weight:bold;color:#ffffff;letter-spacing:-0.5px;\">"
                + "TapeCloud</div>"
                + "<div style=\"font-family:Arial,sans-serif;font-size:12px;color:rgba(255,255,255,0.8);letter-spacing:2px;\">"
                + "CINE &amp; MÚSICA</div></td></tr>"
                + "<tr><td style=\"padding:28px;font-family:Arial,sans-serif;color:#e8edf7;\">"
                + "<div style=\"font-size:16px;font-weight:bold;margin-bottom:8px;\">Tu código de verificación</div>"
                + "<div style=\"font-size:13px;color:#9aa6c2;margin-bottom:20px;\">"
                + "Ingresalo en la app para activar tu cuenta. Vence en 5 minutos.</div>"
                + "<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\"><tr>" + digits + "</tr></table>"
                + "<div style=\"font-size:12px;color:#6b7694;margin-top:20px;\">"
                + "Si no creaste una cuenta en TapeCloud, ignorá este mensaje.</div>"
                + "</td></tr></table>"
                + "<div style=\"font-family:Arial,sans-serif;font-size:11px;color:#4b5570;margin-top:16px;\">"
                + "TapeCloud · Películas y música</div>"
                + "</td></tr></table></body></html>";
    }

    private void sendViaBrevo(String toEmail, String subject, String text, String html) {
        try {
            Map<String, Object> body = Map.of(
                    "sender", Map.of("email", from, "name", brevoSenderName),
                    "to", List.of(Map.of("email", toEmail)),
                    "subject", subject,
                    "textContent", text,
                    "htmlContent", html);
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
