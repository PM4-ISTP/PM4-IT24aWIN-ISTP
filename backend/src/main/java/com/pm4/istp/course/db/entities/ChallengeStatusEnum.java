package com.pm4.istp.course.db.entities;

public enum ChallengeStatusEnum {
  // DRAFT: The challenge is being created and is not yet visible to anyone.
  // PRIVATE: The challenge is visible only to the creator and his courses.
  // PUBLIC: The challenge is visible to all users and can be participated in by anyone.
  DRAFT,
  PRIVATE,
  PUBLIC
}
