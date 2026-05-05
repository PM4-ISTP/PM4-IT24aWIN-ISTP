package com.pm4.istp.course.db.entities;

public enum LabStatusEnum {
  // DRAFT: The lab is being created and is not yet visible to anyone.
  // PRIVATE: The lab is visible only to the creator and his courses.
  // PUBLIC: The lab is visible to all users and can be participated in by anyone.
  DRAFT,
  PRIVATE,
  PUBLIC
}
