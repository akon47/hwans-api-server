package com.hwans.apiserver.entity.attachment;

import org.apache.tika.config.TikaConfig;
import org.apache.tika.mime.MimeTypeException;

public class FileUtils {
    private static TikaConfig config = TikaConfig.getDefaultConfig();

    public static String getExtensionFromMimeType(String mimeType) {
        try {
            return config.getMimeRepository().forName(mimeType).getExtension();
        } catch (MimeTypeException e) {
            return null;
        }
    }
}
