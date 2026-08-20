package com._penLearning.Noddi.global.util;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailAsyncSender {

    private final JavaMailSender mailSender;

    @Async
    public void sendMailAsync(String toEmail, String authCode) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("[Noddi] 이메일 인증번호 안내");

            String content = "<h3>Noddi 서비스 이용을 위한 인증번호입니다.</h3>" +
                    "<br>인증번호: <b>" + authCode + "</b><br>" +
                    "<p>5분 이내에 입력해 주세요.</p>";

            helper.setText(content, true);

            mailSender.send(message);

        } catch (MessagingException | MailException e) {
            // 비동기 스레드 내부에서 예외가 터지므로,
            // API 호출자에게 에러를 던지기보다 로그를 남기는 방식으로 처리합니다.
            log.error("[Email Error] 비동기 메일 발송 실패 (수신자: {}): {}", toEmail, e.getMessage());
        }
    }
}
