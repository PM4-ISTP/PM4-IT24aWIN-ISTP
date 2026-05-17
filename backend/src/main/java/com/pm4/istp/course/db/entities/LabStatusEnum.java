package com.pm4.istp.course.db.entities;

public enum LabStatusEnum {
  // DRAFT: The lab is being created and is not yet visible to anyone.
  // PRIVATE: The lab is visible only to the creator and his courses.
  // PUBLIC: The lab is visible to all users and can be participated in by anyone.
  // SOFT_DELETED: The lab was removed (DB-only) and is filtered from all normal queries.
  DRAFT,
  PRIVATE,
  PUBLIC,
  SOFT_DELETED
}
