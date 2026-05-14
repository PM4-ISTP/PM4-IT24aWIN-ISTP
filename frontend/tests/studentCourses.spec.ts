import test, { expect } from "@playwright/test";
import { clickNavbarButton, loginAs, TestUser } from "@/tests/helpers";

test('"My Courses" tab must be empty, if user is not enrolled in a course.', async ({ page }) => {
  await loginAs(page, TestUser.InstructorWithoutCoursesOrLabs);
  await clickNavbarButton(page, "My Courses", "dashboard/courses");
  await expect(page.getByText("no courses found")).toBeVisible();
});
