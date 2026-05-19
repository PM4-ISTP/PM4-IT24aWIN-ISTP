import { test } from "@/tests/fixtures";
import { clickNavbarButton } from "@/tests/helpers/navigation";
import { testUsers, type User } from "@/tests/data";
import { loginAs } from "@/tests/helpers/auth";
import { assertCourseCards } from "@/tests/helpers/course";

const testCases: { userDesription: string; user: User }[] = [
  {
    userDesription: "student with four courses",
    user: testUsers.student,
  },
  { userDesription: "admin with one course", user: testUsers.admin },
  {
    userDesription: "instructor with zero courses",
    user: testUsers.instructorWithoutCoursesOrLabs,
  },
];

test.describe('Courses on "My Courses" tab must display all courses a user is enrolled in', () => {
  for (const { userDesription, user } of testCases) {
    test(`All enrolled courses get displayed correctly for ${userDesription}`, async ({ page }) => {
      await loginAs(page, user);
      await clickNavbarButton(page, "MY COURSES", "dashboard/courses");
      await assertCourseCards(page, user.enrolledCourses);
    });
  }
});
