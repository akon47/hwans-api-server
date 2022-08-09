package com.hwans.apiserver.entity.blog;

import com.hwans.apiserver.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "tb_tag")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Tag extends BaseEntity {
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;
    @Column(length = 100, unique = true, nullable = false)
    private String name;
    @OneToMany(mappedBy = "tag")
    private final Set<PostTag> postTags = new HashSet<>();

    public Tag(String name) {
        this.name = name;
    }
}
