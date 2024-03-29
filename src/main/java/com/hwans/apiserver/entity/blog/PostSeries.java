package com.hwans.apiserver.entity.blog;

import com.hwans.apiserver.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.io.Serializable;
import java.util.UUID;

@Entity
@Table(name = "tb_post_series")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PostSeries extends BaseEntity implements Serializable {
    @Id
    @OneToOne
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;
    @ManyToOne
    @JoinColumn(name = "series_id", nullable = false)
    private Series series;
    @Column
    private Integer order;
}
