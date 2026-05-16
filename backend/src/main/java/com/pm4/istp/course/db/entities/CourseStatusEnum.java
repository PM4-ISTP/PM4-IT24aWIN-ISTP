package com.pm4.istp.course.db.entities;

public enum CourseStatusEnum {
  // DRAFT: The course is being created and is not visible to students.
  // PRIVATE: The course is visible only to enrolled users (invite-only).
  // PUBLIC: The course is visible to all users in the catalog.
  DRAFT,
  PRIVATE,
  PUBLIC
}

