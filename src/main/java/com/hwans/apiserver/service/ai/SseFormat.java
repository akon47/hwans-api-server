package com.hwans.apiserver.service.ai;

/**
 * SSE(text/event-stream) 프레임을 만든다. 계약서 형식 그대로 {@code "event: x\ndata: {...}\n\n"} 와 {@code ": ping\n\n"}.
 * data 는 JSON 한 줄이지만, 혹시 줄바꿈이 있어도 줄마다 {@code data: } 를 붙여 프레임이 깨지지 않게 한다.
 */
public final class SseFormat {
    private SseFormat() {
    }

    public static String event(String name, String data) {
        var sb = new StringBuilder();
        sb.append("event: ").append(name).append('\n');
        for (var line : data.split("\r\n|\r|\n", -1)) {
            sb.append("data: ").append(line).append('\n');
        }
        return sb.append('\n').toString();
    }

    public static String comment(String text) {
        return ": " + text + "\n\n";
    }
}
