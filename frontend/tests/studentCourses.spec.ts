import test from "@playwright/test";
import { clickNavbarButton } from "@/tests/helpers/navigation";
import { adminCourse01, instructorCourse01, instructorCourse02, instructorCourse04 } from "@/tests/data";
import { loginAs, TestUser } from "@/tests/helpers/auth";
import { assertCourseCards, type Course } from "@/tests/helpers/course";

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
