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

    /**
     * Sends an email with an attachment, custom body, and custom filename.
     *
     * @param to           The recipient's email address.
     * @param bcc          Optional BCC email address.
     * @param subject      The subject of the email.
     * @param body         The plain text body of the email.
     * @param attachmentData The attachment content as byte array.
     * @param fileName     The name of the attachment file (e.g. title.xlsx).
     * @throws MessagingException if sending the email fails.
     */
    public void sendEmailWithAttachment(String to, String bcc, String subject, String body, byte[] attachmentData, String fileName) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setTo(to);
        if (bcc != null && !bcc.isBlank()) {
            helper.setBcc(bcc);
        }

        helper.setSubject(subject);
        helper.setText(body); // Set custom plain text body

        // Attach file with custom name
        helper.addAttachment(fileName, new ByteArrayResource(attachmentData));

        mailSender.send(message);
    }
}
