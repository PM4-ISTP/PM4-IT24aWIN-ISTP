import test from "@playwright/test";
import { assertCourseCards, clickNavbarButton, Course, loginAs, TestUser } from "@/tests/helpers";
import adminCourse01 from "@/tests/files/courses/admin_01.json";
import instructorCourse01 from "@/tests/files/courses/instructor_01.json";
import instructorCourse02 from "@/tests/files/courses/instructor_02.json";
import instructorCourse04 from "@/tests/files/courses/instructor_04.json";

const testUsers: { userDesription: string; user: TestUser; courses: Course[] }[] = [
  {
    userDesription: "student with four courses",
    user: TestUser.Student,
    courses: [instructorCourse02, instructorCourse04, adminCourse01, instructorCourse01],
  },
  { userDesription: "admin with one course", user: TestUser.Admin, courses: [adminCourse01] },
  {
    userDesription: "instructor with zero courses",
    user: TestUser.InstructorWithoutCoursesOrLabs,
    courses: [],
  },
];

test.describe('Courses on "My Courses" tab must display all courses a user is enrolled in', () => {
  for (const { userDesription, user, courses } of testUsers) {
    test(`All enrolled courses get displayed correctly for ${userDesription}`, async ({ page }) => {
      await loginAs(page, user);
      await clickNavbarButton(page, "MY COURSES", "dashboard/courses");
      await assertCourseCards(page, courses);
    });
  }
});
