package com.pm4.istp.course.repositories;

import com.pm4.istp.course.db.entities.SubTaskOption;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubTaskOptionRepository extends JpaRepository<SubTaskOption, UUID> {}
