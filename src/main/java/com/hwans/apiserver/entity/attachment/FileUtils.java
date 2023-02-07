package com.hwans.apiserver.entity.attachment;

import org.apache.tika.config.TikaConfig;
import org.apache.tika.mime.MimeTypeException;

public class FileUtils {
    private static TikaConfig config = TikaConfig.getDefaultConfig();

    /**
     * MimeType으로 파일의 확장자명을 구합니다.
     *
     * @param mimeType MimeType
     * @return MimeType에 따른 확장자명.
     */
    public static String getExtensionFromMimeType(String mimeType) {
        try {
            return config.getMimeRepository().forName(mimeType).getExtension();
        } catch (MimeTypeException e) {
            return null;
        }
    }
}
