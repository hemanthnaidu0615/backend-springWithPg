package com.databin_pg.api.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendEmailWithAttachment(String to, String bcc, String subject, byte[] attachmentData) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setTo(to);
        if (bcc != null && !bcc.isBlank()) {
            helper.setBcc(bcc);
        }

        helper.setSubject(subject);
        helper.setText("Attached is your scheduled report.");

        // Use ByteArrayResource to wrap the byte array data
        helper.addAttachment("report.xlsx", new ByteArrayResource(attachmentData));
        mailSender.send(message);
    }
}
