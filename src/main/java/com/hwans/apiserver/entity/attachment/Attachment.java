package com.hwans.apiserver.entity.attachment;

import com.hwans.apiserver.entity.BaseEntity;
import com.hwans.apiserver.entity.account.Account;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import javax.activation.MimetypesFileTypeMap;
import javax.persistence.*;
import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;

@Entity
@Table(name = "tb_attachment")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Attachment extends BaseEntity {
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;
    @Column(length = 255)
    private String contentType;
    @Column(length = 255)
    private String fileName;
    @Column
    private long fileSize;
    @Column(nullable = false)
    private String localFilePath;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    public Attachment(Account account, File file, String fileName, String contentType) {
        this.account = account;
        this.fileName = fileName == null ? file.getName() : fileName;
        this.fileSize = file.length();
        this.localFilePath = file.getAbsolutePath();
        this.contentType = contentType;
    }

    public String getUrl() {
        var fileTypeWithExt = getFileTypeWithExt();

        if (fileTypeWithExt == null) {
            return "/attachments/" + id;
        } else {
            return "/attachments/" + id + "/" + URLEncoder.encode(fileTypeWithExt, StandardCharsets.UTF_8);
        }
    }

    /**
     * 현재 ContentType에 따른 기본 파일 이름 + 확장자를 반환한다.
     * 이미지, 비디오, 오디오 파일이 아닌 경우에는 업로드 시 사용했던 파일 이름에 확장자가 있는 경우에 해당 파일 이름을 사용한다.
     * 업로드 시 사용했던 파일 이름에 확장자가 없는 경우에는 null을 반환한다.
     *
     * @return 파일 이름 + 확장자
     */
    public String getFileTypeWithExt() {
        var ext = FileUtils.getExtensionFromMimeType(contentType);
        if (ext == null) {
            return null;
        }

        if (contentType.matches("(^image\\/.*)")) {
            return "image" + ext;
        } else if (contentType.matches("(^video\\/.*)")) {
            return "video" + ext;
        } else if (contentType.matches("(^audio\\/.*)")) {
            return "audio" + ext;
        } else {
            int lastIndexOf = fileName.lastIndexOf('.');
            if(lastIndexOf < 0)
                return null;

            return fileName;
        }
    }
}
