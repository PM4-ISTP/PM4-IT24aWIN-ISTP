package com.pm4.istp.service;

import com.pm4.istp.domain.entites.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface UserService {
    Page<User> listInstructorUsers(UUID userId, Pageable pageable);

    Page<User> searchInstructorUsersByName(UUID userId, String name, Pageable pageable);
}
