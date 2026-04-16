package com.pm4.istp.admin.repositories;

import com.pm4.istp.admin.db.AdminConfig;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminConfigRepository extends JpaRepository<AdminConfig, UUID> {}
