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

    public String getExtension() {
        char ch;
        int len;
        if (fileName == null ||
                (len = fileName.length()) == 0 ||
                (ch = fileName.charAt(len - 1)) == '/' || ch == '\\' || //in the case of a directory
                ch == '.') //in the case of . or ..
            return "";
        int dotInd = fileName.lastIndexOf('.'),
                sepInd = Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\'));
        if (dotInd <= sepInd)
            return "";
        else
            return fileName.substring(dotInd + 1).toLowerCase();
    }

    public String getUrl() {
        return "/attachments/" + id;
    }

    public String getFileTypeWithExt() {
        var ext = getExtension();
        if (ext == null)
            return null;

        if (contentType.matches("(^image\\/.*)")) {
            return "image." + ext;
        } else if (contentType.matches("(^video\\/.*)")) {
            return "video." + ext;
        }

        return null;
    }
}
