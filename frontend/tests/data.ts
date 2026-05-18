/*
 * Centralized test data repository
 * All test data is defined here to avoid duplication and maintain a single source of truth.
 * Data is immutable using const and Readonly<T> to prevent accidental modifications.
 */

// ============================================================================
// Type Definitions
// ============================================================================

export type Challenge = Readonly<{
  id: string;
  title: string;
  points: number;
  type: "FLAG" | "MULTIPLE_CHOICE";
  orderIndex: number;
  updatedAt: string;
}>;

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
  challenges: readonly Challenge[];
}>;

export type LabAssignment = Readonly<{
  lab: Lab;
  dueAt: string;
}>;

export type ChallengeCompletion = Readonly<{
  lab: Lab;
  challenge: Challenge;
  submittedAt: string;
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
  completedChallenges: readonly ChallengeCompletion[];
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
  completedChallenges: [],
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
  completedChallenges: [],
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
  completedChallenges: [],
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
  completedChallenges: [],
};

export const testUsers = {
  admin: adminUser,
  instructor: instructorUser,
  instructorWithoutCoursesOrLabs: instructorWithoutCoursesOrLabsUser,
  student: studentUser,
} as const;

// ============================================================================
// Challenge Definitions (E2E labs only)
// ============================================================================

const adminLab01Challenge01: Challenge = {
  id: "73210587-0238-401f-9ccf-e22d4ae4710f",
  title: "E2E Test Course: Admin 01 - Challenge 1",
  points: 1,
  type: "FLAG",
  orderIndex: 0,
  updatedAt: "2026-05-15T11:19:54.049359",
};

const instructorLab01Challenge01: Challenge = {
  id: "0e14ca87-d2b5-45e9-ab04-92eaabbe29eb",
  title: "E2E Test Course: Instructor 01 - Challenge 1",
  points: 1,
  type: "FLAG",
  orderIndex: 0,
  updatedAt: "2026-05-15T11:14:20.527435",
};

const instructorLab02Challenge01: Challenge = {
  id: "f1d2e1c9-eefd-41f5-a195-e70282467a08",
  title: "E2E Test Course: Instructor 02 - Challenge 1",
  points: 3,
  type: "FLAG",
  orderIndex: 0,
  updatedAt: "2026-05-15T11:32:08.776357",
};

const instructorLab02Challenge02: Challenge = {
  id: "3951ffcd-33c7-4473-89ae-719cba79c651",
  title: "E2E Test Course: Instructor 02 - Challenge 2",
  points: 1,
  type: "MULTIPLE_CHOICE",
  orderIndex: 1,
  updatedAt: "2026-05-15T11:32:08.771493",
};

const instructorLab02Challenge03: Challenge = {
  id: "acecc0a4-cf6e-4d75-ad98-265874e2d232",
  title: "E2E Test Course: Instructor 02 - Challenge 3",
  points: 1,
  type: "MULTIPLE_CHOICE",
  orderIndex: 2,
  updatedAt: "2026-05-15T11:32:08.774549",
};

const instructorLab03Challenge01: Challenge = {
  id: "9e9026fd-99c1-4e92-b498-893a3596b28b",
  title: "E2E Test Course: Instructor 03 - Challenge 1",
  points: 1,
  type: "FLAG",
  orderIndex: 0,
  updatedAt: "2026-05-15T11:28:07.33958",
};

const instructorLab03Challenge02: Challenge = {
  id: "54cfcfbf-4baf-4ccd-aeae-90e8ad36acba",
  title: "E2E Test Course: Instructor 03 - Challenge 2",
  points: 3,
  type: "MULTIPLE_CHOICE",
  orderIndex: 1,
  updatedAt: "2026-05-15T11:28:07.335503",
};

const instructorLab03Challenge03: Challenge = {
  id: "c8087132-4aa1-4a9c-ab88-9aa16da20add",
  title: "E2E Test Course: Instructor 03 - Challenge 3",
  points: 2,
  type: "FLAG",
  orderIndex: 2,
  updatedAt: "2026-05-15T11:28:07.338252",
};

const instructorLab04Challenge01: Challenge = {
  id: "7820ac4e-c09b-4269-899a-2b58c7805d22",
  title: "E2E Test Course: Instructor 04 - Challenge 1",
  points: 1,
  type: "FLAG",
  orderIndex: 0,
  updatedAt: "2026-05-15T11:16:23.82805",
};

const instructorLab05Challenge01: Challenge = {
  id: "45268bd3-be53-4453-a4ef-96b158c2ddf5",
  title: "E2E Test Course: Instructor 05 - Challenge 1",
  points: 1,
  type: "MULTIPLE_CHOICE",
  orderIndex: 0,
  updatedAt: "2026-05-16T15:31:31.595004",
};

const instructorLab06Challenge01: Challenge = {
  id: "cbd4884f-0c34-4264-88db-60bb656c108e",
  title: "E2E Test Course: Instructor 06 - Challenge 1",
  points: 2,
  type: "FLAG",
  orderIndex: 0,
  updatedAt: "2026-05-16T16:14:46.102053",
};

const instructorLab06Challenge02: Challenge = {
  id: "324a2ab5-7654-4130-a640-75d5f6e90a59",
  title: "E2E Test Course: Instructor 06 - Challenge 2",
  points: 1,
  type: "FLAG",
  orderIndex: 1,
  updatedAt: "2026-05-16T16:14:46.102595",
};

const instructorLab07Challenge01: Challenge = {
  id: "ab35a542-595d-4025-9aac-f4e6a2a15247",
  title: "E2E Test Course: Instructor 07 - Challenge 1",
  points: 23,
  type: "FLAG",
  orderIndex: 0,
  updatedAt: "2026-05-16T16:18:11.284597",
};

const instructorLab08Challenge01: Challenge = {
  id: "e39d36fa-2ef0-41a1-8f65-357dcd7e34a0",
  title: "E2E Test Course: Instructor 08 - Challenge 1",
  points: 1,
  type: "MULTIPLE_CHOICE",
  orderIndex: 0,
  updatedAt: "2026-05-16T16:20:31.260226",
};

const instructorLab08Challenge02: Challenge = {
  id: "542d02b4-0550-4332-9ab4-1487a2aeb9ca",
  title: "E2E Test Course: Instructor 08 - Challenge 2",
  points: 1,
  type: "MULTIPLE_CHOICE",
  orderIndex: 1,
  updatedAt: "2026-05-16T16:20:31.26121",
};

const instructorLab08Challenge03: Challenge = {
  id: "327ac23a-8a9f-42a4-b015-8d58fb6caf04",
  title: "E2E Test Course: Instructor 08 - Challenge 3",
  points: 2,
  type: "FLAG",
  orderIndex: 2,
  updatedAt: "2026-05-16T16:20:31.262238",
};

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
  challenges: [adminLab01Challenge01],
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
  challenges: [instructorLab01Challenge01],
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
  challenges: [instructorLab02Challenge01, instructorLab02Challenge02, instructorLab02Challenge03],
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
  challenges: [instructorLab03Challenge01, instructorLab03Challenge02, instructorLab03Challenge03],
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
  challenges: [instructorLab04Challenge01],
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
  challenges: [instructorLab05Challenge01],
};

const instructorLab06: Lab = {
  id: "f6a0c1f7-7142-40cb-b24d-d29b44e89f98",
  title: "E2E Test Lab: Instructor 06",
  status: "PUBLIC",
  difficulty: "HARD",
  maxScore: 3,
  dockerImage:
    "ghcr.io/pm4-istp/campus-helpdesk@sha256:fbbd79d166db3439a1751038f4cded971516679e67050dfb9e98f9b3d1e578aa",
  containerPort: 80,
  creatorName: "E2E Instructor",
  courseCount: 1,
  updatedAt: "2026-05-16T16:14:46.101168",
  challenges: [instructorLab06Challenge01, instructorLab06Challenge02],
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
  challenges: [instructorLab07Challenge01],
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
  challenges: [instructorLab08Challenge01, instructorLab08Challenge02, instructorLab08Challenge03],
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
  updatedAt: "2026-05-16T23:33:42.995326",
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
  updatedAt: "2026-05-16T23:13:44.264035",
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
  updatedAt: "2026-05-16T23:17:10.678044",
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
  updatedAt: "2026-05-16T23:18:47.833949",
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
  id: "18c6e7ab-623f-4bb0-bc59-5b8b6560025d",
  title: "E2E Test Course: Instructor 05",
  description:
    "<p>==========</p><p><strong><mark>This is a test course for E2E testing. Please do not interact with this course in any form (e.g. joining or deleting). Otherwise the E2E tests might fail.</mark></strong></p><p>==========</p><p></p><p>Created by: e2e-instructor</p><p>Course nr.: 05</p>",
  shortDescription:
    '"Instructor 05" is a test course for E2E testing. Please do not interact with this course in any form (e.g. joining or deleting). Otherwise the E2E tests might fail.',
  isPublished: false,
  isPrivate: true,
  createdAt: "2026-05-16T16:12:29.824781",
  updatedAt: "2026-05-16T23:19:33.68172",
  topic: "E2E-Testing-02",
  owner: instructorUser,
  private: true,
  published: false,
  numOfParticipants: 1,
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
  updatedAt: "2026-05-16T23:20:36.902283",
  topic: "E2E-Testing-02",
  owner: instructorUser,
  private: true,
  published: false,
  numOfParticipants: 1,
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

export const instructorCourse07: Course = {
  id: "fb394bc6-0673-4df3-8d88-35e0397887bf",
  title: "E2E Test Course: Instructor 07",
  description:
    "<p>==========</p><p><strong><mark>This is a test course for E2E testing. Please do not interact with this course in any form (e.g. joining or deleting). Otherwise the E2E tests might fail.</mark></strong></p><p>==========</p><p></p><p>Created by: e2e-instructor</p><p>Course nr.: 07</p>",
  shortDescription:
    '"Instructor 07" is a test course for E2E testing. Please do not interact with this course in any form (e.g. joining or deleting). Otherwise the E2E tests might fail.',
  isPublished: true,
  isPrivate: false,
  createdAt: "2026-05-16T16:27:52.870031",
  updatedAt: "2026-05-16T23:21:25.933564",
  topic: "E2E-Testing-01",
  owner: instructorUser,
  private: false,
  published: true,
  numOfParticipants: 1,
  labs: [
    {
      lab: instructorLab06,
      dueAt: "2100-01-01T14:00:00",
    },
    {
      lab: instructorLab07,
      dueAt: "2100-01-02T14:00:00",
    },
    {
      lab: instructorLab08,
      dueAt: "2000-01-01T14:00:00",
    },
  ],
};

export const instructorCourse08: Course = {
  id: "daa4fa78-a5ed-4677-99bd-c333eee30c5f",
  title: "E2E Test Course: Instructor 08",
  description:
    "<p>==========</p><p><strong><mark>This is a test course for E2E testing. Please do not interact with this course in any form (e.g. joining or deleting). Otherwise the E2E tests might fail.</mark></strong></p><p>==========</p><p></p><p>Created by: e2e-instructor</p><p>Course nr.: 08</p>",
  shortDescription:
    '"Instructor 08" is a test course for E2E testing. Please do not interact with this course in any form (e.g. joining or deleting). Otherwise the E2E tests might fail.',
  isPublished: true,
  isPrivate: false,
  createdAt: "2026-05-16T21:43:20.235861",
  updatedAt: "2026-05-16T23:22:12.341303",
  topic: "E2E-Testing-01",
  owner: instructorUser,
  private: false,
  published: true,
  numOfParticipants: 0,
  labs: [
    {
      lab: instructorLab01,
      dueAt: "2100-01-01T11:00:00",
    },
    {
      lab: instructorLab03,
      dueAt: "",
    },
    {
      lab: instructorLab05,
      dueAt: "2000-01-01T11:00:00",
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
    completedLabsCount: 4,
  },
  instructor: {
    enrolledCoursesCount: 3,
    completedLabsCount: 3,
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
  instructor07: instructorCourse07,
  instructor08: instructorCourse08,
} as const;

// ============================================================================
// Update User data after course and lab definitions (handle circular dependencies)
//
// This solution uses a workaround for setting in readonly arrays. This isn't
// clean code. But because it is only done once in this file, it is ok.
// ============================================================================

// Admin: enrolls in adminCourse01
(adminUser as unknown as { enrolledCourses: Course[] }).enrolledCourses = [adminCourse01];

// Instructor: enrolls in courses and completes labs
(instructorUser as unknown as { enrolledCourses: Course[] }).enrolledCourses = [
  instructorCourse05,
  instructorCourse02,
  instructorCourse04,
  instructorCourse01,
];
(instructorUser as unknown as { completedLabs: Lab[] }).completedLabs = [
  instructorLab03,
  adminLab01,
  instructorLab05,
];
(instructorUser as unknown as { completedChallenges: ChallengeCompletion[] }).completedChallenges =
  [
    {
      lab: adminLab01,
      challenge: adminLab01Challenge01,
      submittedAt: "2026-05-15T11:26:05.084648",
    },
    {
      lab: instructorLab03,
      challenge: instructorLab03Challenge01,
      submittedAt: "2026-05-15T11:30:37.032344",
    },
    {
      lab: instructorLab03,
      challenge: instructorLab03Challenge02,
      submittedAt: "2026-05-15T11:30:40.430358",
    },
    {
      lab: instructorLab03,
      challenge: instructorLab03Challenge03,
      submittedAt: "2026-05-15T11:30:50.552643",
    },
    {
      lab: instructorLab02,
      challenge: instructorLab02Challenge01,
      submittedAt: "2026-05-15T11:32:24.575717",
    },
    {
      lab: instructorLab02,
      challenge: instructorLab02Challenge02,
      submittedAt: "2026-05-15T11:32:27.145546",
    },
    {
      lab: instructorLab05,
      challenge: instructorLab05Challenge01,
      submittedAt: "2026-05-16T16:21:42.532488",
    },
  ];

// Student: enrolls in courses and completes labs
(studentUser as unknown as { enrolledCourses: Course[] }).enrolledCourses = [
  instructorCourse02,
  instructorCourse04,
  adminCourse01,
  instructorCourse06,
  instructorCourse01,
];
(studentUser as unknown as { completedLabs: Lab[] }).completedLabs = [
  instructorLab02,
  instructorLab06,
  instructorLab07,
  instructorLab08,
];
(studentUser as unknown as { completedChallenges: ChallengeCompletion[] }).completedChallenges = [
  {
    lab: instructorLab02,
    challenge: instructorLab02Challenge01,
    submittedAt: "2026-05-16T15:28:48.50928",
  },
  {
    lab: instructorLab02,
    challenge: instructorLab02Challenge02,
    submittedAt: "2026-05-16T15:28:51.457509",
  },
  {
    lab: instructorLab02,
    challenge: instructorLab02Challenge03,
    submittedAt: "2026-05-16T15:28:54.288624",
  },
  {
    lab: instructorLab06,
    challenge: instructorLab06Challenge01,
    submittedAt: "2026-05-16T16:30:03.670308",
  },
  {
    lab: instructorLab06,
    challenge: instructorLab06Challenge02,
    submittedAt: "2026-05-16T16:30:11.335404",
  },
  {
    lab: instructorLab07,
    challenge: instructorLab07Challenge01,
    submittedAt: "2026-05-16T16:30:21.969296",
  },
  {
    lab: instructorLab08,
    challenge: instructorLab08Challenge01,
    submittedAt: "2026-05-16T16:30:26.963781",
  },
  {
    lab: instructorLab08,
    challenge: instructorLab08Challenge02,
    submittedAt: "2026-05-16T16:30:31.733892",
  },
  {
    lab: instructorLab08,
    challenge: instructorLab08Challenge03,
    submittedAt: "2026-05-16T16:30:40.875401",
  },
];
