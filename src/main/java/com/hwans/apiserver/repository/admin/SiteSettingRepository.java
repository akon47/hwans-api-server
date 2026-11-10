package com.hwans.apiserver.repository.admin;

import com.hwans.apiserver.entity.admin.SiteSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SiteSettingRepository extends JpaRepository<SiteSetting, String> {
}
