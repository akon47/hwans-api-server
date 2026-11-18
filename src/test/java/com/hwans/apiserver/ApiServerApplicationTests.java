package com.hwans.apiserver;

import com.hwans.apiserver.service.ai.AiKeyCipher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertTrue;

// 로컬 프로파일은 JavaMailSender 를 만들려면 spring.mail.host 가 필요하다(CLAUDE.md 의 SPRING_MAIL_HOST). 테스트에서는 여기서 준다.
@SpringBootTest(properties = "spring.mail.host=localhost")
class ApiServerApplicationTests {
	@Autowired
	private AiKeyCipher aiKeyCipher;

	@Test
	void contextLoads() {
		// 로컬 프로파일에는 개발용 AI 마스터 키가 있다
		assertTrue(aiKeyCipher.isEnabled());
	}

}
