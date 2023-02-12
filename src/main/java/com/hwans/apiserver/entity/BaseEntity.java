package com.hwans.apiserver.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.Column;
import javax.persistence.EntityListeners;
import javax.persistence.MappedSuperclass;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;

/**
 * 모든 엔티티가 기본적으로 가지고 있는 공통 엔티티
 */
@Getter
@MappedSuperclass
@SuperBuilder
@EntityListeners(value = {AuditingEntityListener.class})
@NoArgsConstructor
public abstract class BaseEntity {
    @CreatedDate
    @Column(updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public LocalDateTime getLastModifiedAt() {
        return this.updatedAt;
    }
}
