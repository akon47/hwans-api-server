package com.hwans.apiserver.entity.blog;

import com.hwans.apiserver.entity.BaseEntity;
import com.hwans.apiserver.entity.account.Account;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Where;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "tb_comment")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Comment extends BaseEntity {
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;
    @Column
    @Lob
    private String content;
    @Column(nullable = false)
    private boolean deleted;
    // 손님 댓글을 쓴 IP (스팸 차단용. 회원 댓글에는 남기지 않는다)
    @Column(length = 64)
    private String authorIp;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Comment parent;
    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
    @OrderBy(value = "createdAt asc")
    @Where(clause = "deleted = false")
    private final Set<Comment> children = new HashSet<>();
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;
    @ManyToOne
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;
    @OneToMany(mappedBy = "comment")
    private final Set<CommentLike> likes = new HashSet<>();

    public int getChildrenCount() {
        return this.children.size();
    }

    public int getLikeCount() {
        return this.likes.size();
    }

    public UUID getParentId() {
        return Optional.ofNullable(this.parent)
                .map(Comment::getId)
                .orElse(null);
    }

    /**
     * 삭제 상태로 변경합니다.
     */
    public void setDeleted() {
        this.deleted = true;
    }

    public void setAuthor(Account account) {
        this.account = account;
    }

    public void setPost(Post post) {
        this.post = post;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Account getAuthor() {
        return this.account;
    }

    public void setParent(Comment comment) {
        this.parent = comment;
    }

    public void setAuthorIp(String authorIp) {
        this.authorIp = authorIp;
    }
}
