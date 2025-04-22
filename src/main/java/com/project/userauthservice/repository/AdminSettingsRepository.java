package com.project.userauthservice.repository;

import com.project.userauthservice.entity.admin.AdminSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdminSettingsRepository extends JpaRepository<AdminSettings, Long> {
    Optional<AdminSettings> findBySettingKey(String key);
}