package com.referralapp.JobReferralApp.email;

import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;
import java.util.Properties;

@Service
public class EmailService implements EmailSender{

    private final static Logger LOGGER = LoggerFactory.getLogger(EmailService.class);
    private final JavaMailSender mailSender;

    @Autowired
    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    @Async
    public void send(String to, String email) {
        send(to, "Confirm your Email", email);
    }

    // New overloaded method to allow custom subject
    public void send(String to, String subject, String email) {
        try {
            LOGGER.info("Starting to send email to: {}", to);
            
            // Create a custom mail sender with SSL trust
            JavaMailSenderImpl customMailSender = createCustomMailSender();
            
            MimeMessage mimeMessage = customMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");
            helper.setText(email, true);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setFrom("gomathy220604@gmail.com");
            
            LOGGER.info("Email message prepared, attempting to send...");
            customMailSender.send(mimeMessage);
            LOGGER.info("Email sent successfully to: {}", to);
            
        } catch (MessagingException e) {
            LOGGER.error("Failed to send email to: {}. Error: {}", to, e.getMessage(), e);
            throw new IllegalStateException("Failed to send email: " + e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Unexpected error sending email to: {}. Error: {}", to, e.getMessage(), e);
            throw new IllegalStateException("Unexpected error sending email: " + e.getMessage());
        }
    }
    
    private JavaMailSenderImpl createCustomMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost("smtp.gmail.com");
        mailSender.setPort(465);
        mailSender.setUsername("gomathy220604@gmail.com");
        mailSender.setPassword("qywp ntiu hkxv ymoa");
        
        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "false");
        props.put("mail.smtp.ssl.enable", "true");
        props.put("mail.smtp.socketFactory.port", "465");
        props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.socketFactory.fallback", "false");
        props.put("mail.smtp.ssl.trust", "*");
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");
        
        // Trust all certificates for development
        props.put("mail.smtp.ssl.trust", "*");
        props.put("mail.smtp.ssl.checkserveridentity", "false");
        
        return mailSender;
    }
}
