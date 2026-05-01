export const ROLES = {
  ADMINISTRATOR: "ROLE_ADMINISTRATOR",
  INSTRUCTOR: "ROLE_INSTRUCTOR",
  STUDENT: "ROLE_STUDENT",
} as const;

export type Role = (typeof ROLES)[keyof typeof ROLES];

// predefined role groups for easier authorization checks
export const ROLE_GROUPS = {
  ALL: [ROLES.ADMINISTRATOR, ROLES.INSTRUCTOR, ROLES.STUDENT],
  INSTRUCTOR: [ROLES.ADMINISTRATOR, ROLES.INSTRUCTOR],
  ADMIN_ONLY: [ROLES.ADMINISTRATOR],
} as const;
