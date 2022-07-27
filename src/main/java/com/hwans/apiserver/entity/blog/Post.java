package com.hwans.apiserver.entity.blog;

import com.hwans.apiserver.entity.BaseEntity;
import com.hwans.apiserver.entity.account.role.AccountRole;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "tb_post")
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
    @Column(length = 2000, nullable = false)
    private String title;
    @Column
    @Lob
    private String content;
    @OneToMany(mappedBy = "post")
    private final Set<Comment> comments = new HashSet<>();
    @OneToMany(mappedBy = "post")
    private final Set<Like> likes = new HashSet<>();
    @OneToMany(mappedBy = "post")
    private final Set<PostTag> postTags = new HashSet<>();
}
