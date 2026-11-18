package com.hwans.apiserver.service.ai;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class AiPromptsTest {
    @Test
    void rewriteExplainsTheSelectionMarker() {
        var prompt = AiPrompts.rewriteUser("고칠 문장", AiPrompts.rewriteInstruction("polish", null),
                "앞 문단이에요. [다듬을 부분] 뒤 문단이에요.");
        assertTrue(prompt.contains("앞 문단이에요. [다듬을 부분] 뒤 문단이에요."));
        assertTrue(prompt.contains("`[다듬을 부분]` 표시가 아래 <바꿀 텍스트>가 들어가는 자리"), prompt);
        assertTrue(prompt.contains("<바꿀 텍스트>\n고칠 문장\n</바꿀 텍스트>"));
        assertTrue(prompt.endsWith("`[다듬을 부분]` 표시, 앞뒤 문맥, 태그, 설명은 출력하지 마세요."), prompt);
        // 문맥이 없으면 문맥 안내도 없다
        assertFalse(AiPrompts.rewriteUser("x", "y", null).contains("<앞뒤 문맥>"));
    }

    @Test
    void draftPromptsCarryTheContractRules() {
        var system = AiPrompts.draftSystem("김환", "akon47", null, java.util.List.of(new AiPrompts.PostExcerpt("예전 글", "본문")));
        assertTrue(system.contains("아직 공식 정보가 없어요"));
        assertTrue(system.contains("익스플로잇"));
        assertTrue(system.contains("NVD"));
        assertTrue(system.contains("맨 위에 `#` 제목을 쓰지 말고"));
        assertTrue(system.contains(AiPrompts.NO_STYLE));
        var user = AiPrompts.draftUser("CVE 정리", null, "long", true, LocalDate.of(2026, 11, 18));
        assertTrue(user.startsWith("오늘 날짜: 2026-11-18"));
        assertTrue(user.contains("## 참고 자료"));
        assertTrue(user.contains("6,000자"));
    }
}
