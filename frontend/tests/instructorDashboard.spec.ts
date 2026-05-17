import test, { expect } from "@playwright/test";
import { clickNavbarButton } from "@/tests/helpers/navigation";
import { loginAs } from "@/tests/helpers/auth";
import { courses, testUsers } from "@/tests/data";
import { assertCourseCards } from "@/tests/helpers/course";

const instructorCourses = [
  courses.instructor05,
  courses.instructor02,
  courses.instructor04,
  courses.instructor06,
  courses.instructor03,
  courses.instructor08,
  courses.instructor01,
  courses.instructor07,
];

const adminCourses = [courses.admin01];

const testCasesAllCoursesGetDisplayed = [
  { testUser: testUsers.instructor, courses: instructorCourses },
  { testUser: testUsers.admin, courses: adminCourses },
];

test("Instructor dashboard (instructor courses tab) must be empty, if user is not an instructor of any courses.", async ({
  page,
}) => {
  await loginAs(page, testUsers.instructorWithoutCoursesOrLabs);
  await clickNavbarButton(page, "Dashboard", "dashboard/instructor");
  await expect(page.getByText("no courses found")).toBeVisible();
});

test.describe("Instructor dashboard must display the courses of the given instructor.", () => {
  for (const testCase of testCasesAllCoursesGetDisplayed) {
    test(`All courses of user ${testCase.testUser.username} get displayed.`, async ({ page }) => {
      await loginAs(page, testCase.testUser);
      await clickNavbarButton(page, "Dashboard", "dashboard/instructor");
      await assertCourseCards(page, testCase.courses);
    });
  }
});
