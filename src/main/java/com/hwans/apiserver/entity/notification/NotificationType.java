package com.hwans.apiserver.entity.notification;


import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 알림 유형
 */
@Getter
@RequiredArgsConstructor
public enum NotificationType {
    /**
     * 새로운 댓글 알림
     */
    COMMENT(Values.COMMENT);

    private final String name;

    public static class Values {
        /**
         * 새로운 댓글 알림
         */
        public static final String COMMENT = "NOTIFICATION_COMMENT";
    }
}
