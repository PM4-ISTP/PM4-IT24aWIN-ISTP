/*
 * Centralized test data repository
 * All test data is defined here to avoid duplication and maintain a single source of truth.
 * Data is immutable using const and Readonly<T> to prevent accidental modifications.
 */

// ============================================================================
// Type Definitions
// ============================================================================

export type Lab = Readonly<{
  id: string;
  title: string;
  status: "PUBLIC" | "PRIVATE" | "DRAFT";
  difficulty: string;
  maxScore: number;
  dockerImage: string;
  containerPort: number;
  creatorName: string;
  courseCount: number;
  updatedAt: string;
}>;

export type LabAssignment = Readonly<{
  lab: Lab;
  dueAt: string;
}>;

export type User = Readonly<{
  id: string;
  username: string;
  password: string;
  name: string;
  role: "Admin" | "Instructor" | "Student";
  title: string;
  enrolledCourses: readonly Course[];
  completedLabs: readonly Lab[];
}>;

export type Course = Readonly<{
  id: string;
  title: string;
  description: string;
  shortDescription: string;
  isPublished: boolean;
  isPrivate: boolean;
  createdAt: string;
  updatedAt: string;
  topic: string;
  owner: User;
  private: boolean;
  published: boolean;
  numOfParticipants: number;
  labs: readonly LabAssignment[];
}>;

export type DashboardTestDataEntry = Readonly<{
  enrolledCoursesCount: number;
  completedLabsCount: number;
}>;

// ============================================================================
// User Definitions (Forward declarations to handle circular dependencies)
// ============================================================================

// Admin User
const adminUser: User = {
  id: "f730669a-055b-4362-8a01-605d9881c5b0",
  username: process.env.E2E_ADMIN_USERNAME ?? "e2e-admin",
  password: process.env.E2E_ADMIN_PASSWORD ?? "e2e-admin",
  name: "E2E Admin",
  role: "Admin",
  title: "Test Administrator",
  enrolledCourses: [],
  completedLabs: [],
};

// Instructor User
const instructorUser: User = {
  id: "e4f2814e-0bd9-4fe9-acd3-8d08cfb11179",
  username: process.env.E2E_INSTRUCTOR_USERNAME ?? "e2e-instructor",
  password: process.env.E2E_INSTRUCTOR_PASSWORD ?? "e2e-instructor",
  name: "E2E Instructor",
  role: "Instructor",
  title: "Test Instructor",
  enrolledCourses: [],
  completedLabs: [],
};

// Instructor Without Courses Or Labs (separate user with Instructor role)
const instructorWithoutCoursesOrLabsUser: User = {
  id: "a1b2c3d4-e5f6-4g7h-8i9j-0k1l2m3n4o5p",
  username:
    process.env.E2E_INSTRUCTOR_WITHOUT_COURSES_OR_LABS_USERNAME ??
    "e2e-instructor-without-courses-or-labs",
  password:
    process.env.E2E_INSTRUCTOR_WITHOUT_COURSES_OR_LABS_PASSWORD ??
    "e2e-instructor-without-courses-or-labs",
  name: "E2E Instructor Without Courses",
  role: "Instructor",
  title: "Test Instructor",
  enrolledCourses: [],
  completedLabs: [],
};

// Student User
const studentUser: User = {
  id: "b2c3d4e5-f6a7-4b8c-9d0e-1f2a3b4c5d6e",
  username: process.env.E2E_STUDENT_USERNAME ?? "e2e-student",
  password: process.env.E2E_STUDENT_PASSWORD ?? "e2e-student",
  name: "E2E Student",
  role: "Student",
  title: "Student",
  enrolledCourses: [],
  completedLabs: [],
};

export const testUsers = {
  admin: adminUser,
  instructor: instructorUser,
  instructorWithoutCoursesOrLabs: instructorWithoutCoursesOrLabsUser,
  student: studentUser,
} as const;

// ============================================================================
// Lab Definitions
// ============================================================================

const adminLab01: Lab = {
  id: "e93b357d-b0f7-4f51-8abf-d6b1890cdb75",
  title: "E2E Test Lab: Admin 01",
  status: "PUBLIC",
  difficulty: "MEDIUM",
  maxScore: 1,
  dockerImage:
    "ghcr.io/pm4-istp/campus-helpdesk@sha256:fbbd79d166db3439a1751038f4cded971516679e67050dfb9e98f9b3d1e578aa",
  containerPort: 80,
  creatorName: "E2E Admin",
  courseCount: 1,
  updatedAt: "2026-05-15T11:23:40.18509",
};

const instructorLab01: Lab = {
  id: "61b8867c-14d6-4a1e-b405-6238ef82513d",
  title: "E2E Test Lab: Instructor 01",
  status: "PUBLIC",
  difficulty: "MEDIUM",
  maxScore: 1,
  dockerImage:
    "ghcr.io/pm4-istp/campus-helpdesk@sha256:fbbd79d166db3439a1751038f4cded971516679e67050dfb9e98f9b3d1e578aa",
  containerPort: 80,
  creatorName: "E2E Instructor",
  courseCount: 1,
  updatedAt: "2026-05-15T11:23:04.107777",
};

const instructorLab02: Lab = {
  id: "4ad6bb3d-2fc4-4e45-848a-e044410d11f6",
  title: "E2E Test Lab: Instructor 02",
  status: "PRIVATE",
  difficulty: "MEDIUM",
  maxScore: 5,
  dockerImage:
    "ghcr.io/pm4-istp/campus-helpdesk@sha256:fbbd79d166db3439a1751038f4cded971516679e67050dfb9e98f9b3d1e578aa",
  containerPort: 80,
  creatorName: "E2E Instructor",
  courseCount: 3,
  updatedAt: "2026-05-15T11:32:08.776083",
};

const instructorLab03: Lab = {
  id: "2d975668-2741-4d61-b02e-24cd2528a480",
  title: "E2E Test Lab: Instructor 03",
  status: "PUBLIC",
  difficulty: "MEDIUM",
  maxScore: 6,
  dockerImage:
    "ghcr.io/pm4-istp/campus-helpdesk@sha256:fbbd79d166db3439a1751038f4cded971516679e67050dfb9e98f9b3d1e578aa",
  containerPort: 80,
  creatorName: "E2E Instructor",
  courseCount: 2,
  updatedAt: "2026-05-15T11:28:07.339331",
};

const instructorLab04: Lab = {
  id: "d71ff207-c4c0-428e-ae71-41f338989180",
  title: "E2E Test Lab: Instructor 04",
  status: "DRAFT",
  difficulty: "MEDIUM",
  maxScore: 1,
  dockerImage:
    "ghcr.io/pm4-istp/campus-helpdesk@sha256:fbbd79d166db3439a1751038f4cded971516679e67050dfb9e98f9b3d1e578aa",
  containerPort: 80,
  creatorName: "E2E Instructor",
  courseCount: 0,
  updatedAt: "2026-05-15T11:23:17.275862",
};

const instructorLab05: Lab = {
  id: "2378cb15-0151-4109-97a9-eb3a5b40a022",
  title: "E2E Test Lab: Instructor 05",
  status: "PRIVATE",
  difficulty: "EASY",
  maxScore: 1,
  dockerImage:
    "ghcr.io/pm4-istp/campus-helpdesk@sha256:fbbd79d166db3439a1751038f4cded971516679e67050dfb9e98f9b3d1e578aa",
  containerPort: 80,
  creatorName: "E2E Instructor",
  courseCount: 1,
  updatedAt: "2026-05-16T15:58:20.374834",
};

const instructorLab06: Lab = {
  id: "f6a0c1f7-7142-40cb-b24d-d29b44e89f98",
  title: "E2E Test Lab: Instructor 06",
  status: "PUBLIC",
  difficulty: "HARD",
  maxScore: 1,
  dockerImage:
    "ghcr.io/pm4-istp/campus-helpdesk@sha256:fbbd79d166db3439a1751038f4cded971516679e67050dfb9e98f9b3d1e578aa",
  containerPort: 80,
  creatorName: "E2E Instructor",
  courseCount: 1,
  updatedAt: "2026-05-16T16:14:46.101168",
};

const instructorLab07: Lab = {
  id: "09e8ddaa-4cc2-4c0e-9127-906973f9727b",
  title: "E2E Test Lab: Instructor 07",
  status: "PRIVATE",
  difficulty: "EXPERT",
  maxScore: 23,
  dockerImage:
    "ghcr.io/pm4-istp/campus-helpdesk@sha256:fbbd79d166db3439a1751038f4cded971516679e67050dfb9e98f9b3d1e578aa",
  containerPort: 80,
  creatorName: "E2E Instructor",
  courseCount: 1,
  updatedAt: "2026-05-16T16:18:11.284093",
};

const instructorLab08: Lab = {
  id: "d1e3d1b9-ba3a-4fe9-9545-cdef24fa99b5",
  title: "E2E Test Lab: Instructor 08",
  status: "PUBLIC",
  difficulty: "BEGINNER",
  maxScore: 4,
  dockerImage:
    "ghcr.io/pm4-istp/campus-helpdesk@sha256:fbbd79d166db3439a1751038f4cded971516679e67050dfb9e98f9b3d1e578aa",
  containerPort: 80,
  creatorName: "E2E Instructor",
  courseCount: 1,
  updatedAt: "2026-05-16T16:20:31.259556",
};

// ============================================================================
// Course Definitions (with Lab References and Deadlines)
// ============================================================================

export const adminCourse01: Course = {
  id: "b4ef3fb0-c2e0-4db4-a4aa-f8298662db6a",
  title: "E2E Test Course: Admin 01",
  description:
    "<p>==========</p><p><strong><mark>This is a test course for E2E testing. Please do not interact with this course in any form (e.g. joining or deleting). Otherwise the E2E tests might fail.</mark></strong></p><p>==========</p><p></p><p>Created by: e2e-admin</p><p>Course nr.: 01</p>",
  shortDescription:
    '"Admin 01" is a test course for E2E testing. Please do not interact with this course in any form (e.g. joining or deleting). Otherwise the E2E tests might fail.',
  isPublished: true,
  isPrivate: false,
  createdAt: "2026-05-14T12:34:27.264592",
  updatedAt: "2026-05-14T12:42:21.790117",
  topic: "E2E-Testing-01",
  owner: adminUser,
  private: false,
  published: true,
  numOfParticipants: 2,
  labs: [],
};

export const instructorCourse01: Course = {
  id: "e2bf8cc9-366f-4617-a12b-245507395be9",
  title: "E2E Test Course: Instructor 01",
  description:
    "<p>==========</p><p><strong><mark>This is a test course for E2E testing. Please do not interact with this course in any form (e.g. joining or deleting). Otherwise the E2E tests might fail.</mark></strong></p><p>==========</p><p></p><p>Created by: e2e-instructor</p><p>Course nr.: 01</p>",
  shortDescription:
    '"Instructor 01" is a test course for E2E testing. Please do not interact with this course in any form (e.g. joining or deleting). Otherwise the E2E tests might fail.',
  isPublished: true,
  isPrivate: false,
  createdAt: "2026-05-14T12:39:30.044949",
  updatedAt: "2026-05-15T11:17:29.9017",
  topic: "E2E-Testing-01",
  owner: instructorUser,
  private: false,
  published: true,
  numOfParticipants: 2,
  labs: [
    {
      lab: instructorLab01,
      dueAt: "2100-01-01T11:00:00",
    },
    {
      lab: instructorLab02,
      dueAt: "2000-01-01T11:00:00",
    },
    {
      lab: instructorLab03,
      dueAt: "2100-01-02T11:00:00",
    },
    {
      lab: instructorLab05,
      dueAt: "2000-01-02T11:00:00",
    },
  ],
};

export const instructorCourse02: Course = {
  id: "5c23ae34-88af-4d46-96e5-a412d019a97b",
  title: "E2E Test Course: Instructor 02",
  description:
    "<p>==========</p><p><strong><mark>This is a test course for E2E testing. Please do not interact with this course in any form (e.g. joining or deleting). Otherwise the E2E tests might fail.</mark></strong></p><p>==========</p><p></p><p>Created by: e2e-instructor</p><p>Course nr.: 02</p>",
  shortDescription:
    '"Instructor 02" is a test course for E2E testing. Please do not interact with this course in any form (e.g. joining or deleting). Otherwise the E2E tests might fail.',
  isPublished: false,
  isPrivate: true,
  createdAt: "2026-05-14T12:40:07.002102",
  updatedAt: "2026-05-15T11:18:08.1631",
  topic: "E2E-Testing-02",
  owner: instructorUser,
  private: true,
  published: false,
  numOfParticipants: 2,
  labs: [
    {
      lab: instructorLab03,
      dueAt: "",
    },
    {
      lab: adminLab01,
      dueAt: "2000-01-03T13:00:00",
    },
    {
      lab: instructorLab02,
      dueAt: "2100-01-02T13:00:00",
    },
    {
      lab: instructorLab05,
      dueAt: "2100-01-03T13:00:00",
    },
  ],
};

export const instructorCourse03: Course = {
  id: "d00f6cec-fe95-4ca6-a50a-42c0d543025d",
  title: "E2E Test Course: Instructor 03",
  description:
    "<p>==========</p><p><strong><mark>This is a test course for E2E testing. Please do not interact with this course in any form (e.g. joining or deleting). Otherwise the E2E tests might fail.</mark></strong></p><p>==========</p><p></p><p>Created by: e2e-instructor</p><p>Course nr.: 03</p>",
  shortDescription:
    '"Instructor 03" is a test course for E2E testing. Please do not interact with this course in any form (e.g. joining or deleting). Otherwise the E2E tests might fail.',
  isPublished: false,
  isPrivate: false,
  createdAt: "2026-05-14T12:40:45.551012",
  updatedAt: "2026-05-15T15:26:09.989781",
  topic: "E2E-Testing-01",
  owner: instructorUser,
  private: false,
  published: false,
  numOfParticipants: 1,
  labs: [],
};

export const instructorCourse04: Course = {
  id: "6fa36d31-2418-48af-afca-b8576ac978ec",
  title: "E2E Test Course: Instructor 04",
  description:
    "<p>==========</p><p><strong><mark>This is a test course for E2E testing. Please do not interact with this course in any form (e.g. joining or deleting). Otherwise the E2E tests might fail.</mark></strong></p><p>==========</p><p></p><p>Created by: e2e-instructor</p><p>Course nr.: 04</p>",
  shortDescription:
    '"Instructor 04" is a test course for E2E testing. Please do not interact with this course in any form (e.g. joining or deleting). Otherwise the E2E tests might fail.',
  isPublished: true,
  isPrivate: false,
  createdAt: "2026-05-14T12:41:39.618199",
  updatedAt: "2026-05-15T11:23:59.909511",
  topic: "",
  owner: instructorUser,
  private: false,
  published: true,
  numOfParticipants: 2,
  labs: [
    {
      lab: instructorLab02,
      dueAt: "",
    },
  ],
};

export const instructorCourse05: Course = {
  id: "6fa36d31-2418-48af-afca-b8576ac978ec",
  title: "E2E Test Course: Instructor 05",
  description:
    "<p>==========</p><p><strong><mark>This is a test course for E2E testing. Please do not interact with this course in any form (e.g. joining or deleting). Otherwise the E2E tests might fail.</mark></strong></p><p>==========</p><p></p><p>Created by: e2e-instructor</p><p>Course nr.: 05</p>",
  shortDescription:
    '"Instructor 05" is a test course for E2E testing. Please do not interact with this course in any form (e.g. joining or deleting). Otherwise the E2E tests might fail.',
  isPublished: true,
  isPrivate: false,
  createdAt: "2026-05-14T12:41:39.618199",
  updatedAt: "2026-05-15T11:23:59.909511",
  topic: "",
  owner: instructorUser,
  private: false,
  published: true,
  numOfParticipants: 2,
  labs: [],
};

export const instructorCourse06: Course = {
  id: "b5d31d5d-5b59-44be-9f5b-e20cea9be92b",
  title: "E2E Test Course: Instructor 06",
  description:
    "<p>==========</p><p><strong><mark>This is a test course for E2E testing. Please do not interact with this course in any form (e.g. joining or deleting). Otherwise the E2E tests might fail.</mark></strong></p><p>==========</p><p></p><p>Created by: e2e-instructor</p><p>Course nr.: 06</p>",
  shortDescription:
    '"Instructor 06" is a test course for E2E testing. Please do not interact with this course in any form (e.g. joining or deleting). Otherwise the E2E tests might fail.',
  isPublished: false,
  isPrivate: true,
  createdAt: "2026-05-16T16:27:52.870031",
  updatedAt: "2026-05-16T16:28:41.927021",
  topic: "E2E-Testing-02",
  owner: instructorUser,
  private: true,
  published: false,
  numOfParticipants: 2,
  labs: [
    {
      lab: instructorLab06,
      dueAt: "2026-05-16T18:35:00",
    },
    {
      lab: instructorLab07,
      dueAt: "2026-05-16T18:35:00",
    },
    {
      lab: instructorLab08,
      dueAt: "2026-05-16T18:40:00",
    },
  ],
};

// ============================================================================
// Dashboard Test Data (statistics only)
// ============================================================================

export const dashboardTestData: Readonly<{
  instructorWithoutCoursesOrLabs: DashboardTestDataEntry;
  student: DashboardTestDataEntry;
  instructor: DashboardTestDataEntry;
  admin: DashboardTestDataEntry;
}> = {
  instructorWithoutCoursesOrLabs: {
    enrolledCoursesCount: 0,
    completedLabsCount: 0,
  },
  student: {
    enrolledCoursesCount: 4,
    completedLabsCount: 0,
  },
  instructor: {
    enrolledCoursesCount: 3,
    completedLabsCount: 2,
  },
  admin: {
    enrolledCoursesCount: 1,
    completedLabsCount: 0,
  },
};

// ============================================================================
// Exported Lab Collections (for convenience)
// ============================================================================

export const labs = {
  admin01: adminLab01,
  instructor01: instructorLab01,
  instructor02: instructorLab02,
  instructor03: instructorLab03,
  instructor04: instructorLab04,
  instructor05: instructorLab05,
  instructor06: instructorLab06,
  instructor07: instructorLab07,
  instructor08: instructorLab08,
} as const;

// ============================================================================
// Exported Course Collections (for convenience)
// ============================================================================

export const courses = {
  admin01: adminCourse01,
  instructor01: instructorCourse01,
  instructor02: instructorCourse02,
  instructor03: instructorCourse03,
  instructor04: instructorCourse04,
  instructor05: instructorCourse05,
  instructor06: instructorCourse06,
} as const;

// ============================================================================
// Update User data after course and lab definitions (handle circular dep)
//
// This solution uses a workaround for setting in readonly arrays. This isn't
// clean code. But because it is only done once in this file, it is ok.
// ============================================================================

// Admin: enrolls in adminCourse01
(adminUser as unknown as { enrolledCourses: Course[] }).enrolledCourses = [adminCourse01];

// Instructor: enrolls in courses and completes labs
(instructorUser as unknown as { enrolledCourses: Course[] }).enrolledCourses = [
  instructorCourse02,
  instructorCourse04,
  instructorCourse01,
];
(instructorUser as unknown as { completedLabs: Lab[] }).completedLabs = [
  instructorLab03,
  adminLab01,
  instructorLab05,
];

// Student: enrolls in courses and completes labs
(studentUser as unknown as { enrolledCourses: Course[] }).enrolledCourses = [
  instructorCourse02,
  instructorCourse04,
  adminCourse01,
  instructorCourse01,
];
(studentUser as unknown as { completedLabs: Lab[] }).completedLabs = [
  instructorLab02,
  instructorLab06,
  instructorLab07,
  instructorLab08,
];
