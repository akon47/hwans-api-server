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
@Table(name = "tb_series", uniqueConstraints = @UniqueConstraint(columnNames = {"account_id", "seriesUrl"}))
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Series extends BaseEntity {
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;
    @Column(length = 320, nullable = false)
    private String seriesUrl;
    @Column(length = 2000, nullable = false)
    private String title;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;
    @OneToMany(mappedBy = "series", cascade = CascadeType.PERSIST, orphanRemoval = true)
    @OrderBy(value = "createdAt asc")
    private final List<PostSeries> postSeries = new ArrayList<>();

    public void setAuthor(Account account) {
        this.account = account;
    }

    public void setSeriesUrl(String seriesUrl) {
        this.seriesUrl = seriesUrl;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<Post> getPosts() {
        return postSeries.stream().map(PostSeries::getPost).collect(Collectors.toList());
    }

    public int getPostCount() {
        return this.postSeries.size();
    }
}
