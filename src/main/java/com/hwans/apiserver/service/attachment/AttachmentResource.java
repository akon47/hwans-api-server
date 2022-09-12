package com.hwans.apiserver.service.attachment;

import com.hwans.apiserver.entity.attachment.Attachment;
import lombok.Getter;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

@Getter
public class AttachmentResource extends InputStreamResource {
    private final MediaType contentType;
    private final long contentLength;
    private final String fileName;

    public AttachmentResource(Attachment attachment) throws IOException {
        super(Files.newInputStream(Paths.get(attachment.getLocalFilePath())));
        this.contentType = MediaType.valueOf(attachment.getContentType());
        this.contentLength = attachment.getFileSize();
        this.fileName = attachment.getFileName();
    }

    @Override
    public long contentLength() {
        return this.contentLength;
    }
}
