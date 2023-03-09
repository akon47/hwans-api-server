package com.hwans.apiserver.entity.blog;

import com.hwans.apiserver.entity.BaseEntity;
import com.hwans.apiserver.entity.account.Account;
import com.hwans.apiserver.entity.attachment.Attachment;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Where;

import javax.persistence.*;
import java.util.*;
import java.util.stream.Collectors;

@Entity
@Table(name = "tb_post", uniqueConstraints = @UniqueConstraint(columnNames = {"account_id", "postUrl"}))
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Post extends BaseEntity {
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;
    @Column(length = 320, nullable = false)
    private String postUrl;
    @Column(length = 2000, nullable = false)
    private String title;
    @Column(length = 255)
    private String summary;
    @Column
    @Lob
    private String content;
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private OpenType openType;
    @OneToOne
    @JoinColumn(name = "thumbnail_image_file_id")
    private Attachment thumbnailImage;
    @Column(nullable = false)
    private boolean deleted;
    @Column
    private Integer hits;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;
    @OneToMany(mappedBy = "post")
    @Where(clause = "deleted = false and parent_id is null")
    @OrderBy(value = "createdAt asc")
    private final Set<Comment> comments = new HashSet<>();
    @OneToMany(mappedBy = "post")
    private final Set<Like> likes = new HashSet<>();
    @OneToMany(mappedBy = "post", cascade = CascadeType.PERSIST, orphanRemoval = true)
    @Getter(AccessLevel.NONE)
    private final Set<PostTag> postTags = new HashSet<>();
    @OneToOne(mappedBy = "post")
    private PostSeries postSeries;

    public void setAuthor(Account account) {
        this.account = account;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setOpenType(OpenType openType) {
        this.openType = openType;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public void setTags(Collection<Tag> tags) {
        this.postTags.clear();
        tags.forEach(tag -> this.postTags.add(PostTag.builder().post(this).tag(tag).build()));
    }

    /**
     * 삭제 상태로 변경합니다.
     */
    public void setDeleted() {
        this.deleted = true;
    }

    public void setPostUrl(String postUrl) {
        this.postUrl = postUrl;
    }

    public void updatePostUrlIfNecessary() {
        if (this.postUrl == null || this.postUrl.isBlank()) {
            setPostUrl(UUID.randomUUID().toString().replace("-", "").substring(0, 8));
        }
    }

    public Set<Tag> getTags() {
        return postTags.stream().map(PostTag::getTag).collect(Collectors.toSet());
    }

    public int getLikeCount() {
        return this.likes.size();
    }

    public int getCommentCount() {
        return this.comments.size();
    }

    public Account getAuthor() {
        return this.account;
    }

    public String getThumbnailImageUrl() {
        return Optional.ofNullable(this.thumbnailImage)
                .map(Attachment::getUrl)
                .orElse(null);
    }

    public void setThumbnailImage(Attachment attachment) {
        this.thumbnailImage = attachment;
    }

    public void setHits(Integer hits) {
        this.hits = hits;
    }

    public String getSeriesUrl() {
        return Optional.ofNullable(postSeries)
                .map(PostSeries::getSeries)
                .map(Series::getSeriesUrl)
                .orElse(null);
    }
}
