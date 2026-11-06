package com.hwans.apiserver.service.mail;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Resend(https://resend.com) HTTP API 로 메일을 보낸다.
 * 자체 메일 서버(SMTP) 대신 쓴다. API 키가 비어 있으면 비활성 상태이며 호출하는 쪽이 SMTP 로 대신 보낸다.
 *
 * 설정: mail.resend.api-key (비밀, 운영 VM 의 application-secret.yml), mail.resend.from (보내는 사람. 도메인이 Resend 에서 인증돼 있어야 한다)
 */
@Component
@Slf4j
public class ResendMailClient {
    private static final String ENDPOINT = "https://api.resend.com/emails";

    private final String apiKey;
    private final String from;
    private final RestClient restClient;

    public ResendMailClient(@Value("${mail.resend.api-key:}") String apiKey,
                            @Value("${mail.resend.from:Hwan'Story <noreply@kimhwan.kr>}") String from) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.from = from;
        this.restClient = RestClient.builder().baseUrl(ENDPOINT).build();
    }

    public boolean isEnabled() {
        return !apiKey.isEmpty();
    }

    /**
     * 메일 한 통을 보낸다. 실패하면 예외를 던진다 (호출하는 쪽에서 로그·재시도 판단).
     */
    public void send(String to, String subject, String content, boolean html) {
        Map<String, Object> body = html
                ? Map.of("from", from, "to", List.of(to), "subject", subject, "html", content)
                : Map.of("from", from, "to", List.of(to), "subject", subject, "text", content);
        var response = restClient.post()
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toEntity(String.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("Resend 응답 " + response.getStatusCode());
        }
        log.debug("Resend 메일 전송 완료: to={}, subject={}", to, subject);
    }
}
