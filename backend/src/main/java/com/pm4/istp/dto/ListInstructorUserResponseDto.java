package com.pm4.istp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ListInstructorUserResponseDto {
    private UUID id;
    private String name;
}
