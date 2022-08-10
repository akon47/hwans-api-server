package com.hwans.apiserver.entity.blog;

import com.hwans.apiserver.dto.blog.PostRequestDto;
import com.hwans.apiserver.entity.BaseEntity;
import com.hwans.apiserver.entity.account.Account;
import com.hwans.apiserver.entity.account.role.AccountRole;
import com.hwans.apiserver.entity.account.role.Role;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Table(name = "tb_post", uniqueConstraints = @UniqueConstraint(columnNames = {"blogId", "postUrl"}))
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
    @Column(length = 32, nullable = false)
    private String blogId;
    @Column(length = 320, nullable = false)
    private String postUrl;
    @Column(length = 2000, nullable = false)
    private String title;
    @Column
    @Lob
    private String content;
    @Column(nullable = false)
    private boolean deleted;
    @ManyToOne
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;
    @OneToMany(mappedBy = "post")
    private final Set<Comment> comments = new HashSet<>();
    @OneToMany(mappedBy = "post")
    private final Set<Like> likes = new HashSet<>();
    @OneToMany(mappedBy = "post", cascade = CascadeType.PERSIST, orphanRemoval = true)
    @Getter(AccessLevel.NONE)
    private final Set<PostTag> postTags = new HashSet<>();

    public void modify(PostRequestDto postRequestDto) {
        this.title = postRequestDto.getTitle();
        this.content = postRequestDto.getContent();
    }

    public void setAuthor(Account account) {
        this.account = account;
        this.blogId = account.getBlogId();
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setTags(Collection<Tag> tags) {
        this.postTags.clear();
        tags.stream().forEach(tag -> this.postTags.add(PostTag.builder().post(this).tag(tag).build()));
    }

    public void setDelete() {
        this.deleted = true;
    }

    public Set<Tag> getTags() {
        return postTags.stream().map(x -> x.getTag()).collect(Collectors.toSet());
    }
}
