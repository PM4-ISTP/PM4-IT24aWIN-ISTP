package com.pm4.istp.domain.entites;

import java.util.Arrays;
import java.util.Optional;

public enum UserRoleEnum {
    ROLE_ADMINISTRATOR,
    ROLE_INSTRUCTOR,
    ROLE_STUDENT;

    public static Optional<UserRoleEnum> fromString(String role) {
        return Arrays.stream(values())
                .filter(r -> r.name().equals(role))
                .findFirst();
    }
}
