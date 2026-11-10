package com.hwans.apiserver.entity.admin;

import com.hwans.apiserver.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사이트 설정 엔티티 (이름 -> 값. 값은 JSON 문자열)
 */
@Entity
@Table(name = "tb_site_setting")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SiteSetting extends BaseEntity {
    @Id
    @Column(name = "setting_key", length = 64)
    private String key;
    @Column(name = "setting_value", length = 4000)
    private String value;

    public void setValue(String value) {
        this.value = value;
    }
}
