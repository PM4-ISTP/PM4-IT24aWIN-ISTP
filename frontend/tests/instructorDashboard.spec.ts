import test, { expect } from "@playwright/test";
import { clickNavbarButton } from "@/tests/helpers/navigation";
import { loginAs } from "@/tests/helpers/auth";
import { testUsers } from "@/tests/data";

test("Instructor dashboard (instructor courses tab) must be empty, if user is not an instructor of any courses.", async ({
  page,
}) => {
  await loginAs(page, testUsers.instructorWithoutCoursesOrLabs);
  await clickNavbarButton(page, "Dashboard", "dashboard/instructor");
  await expect(page.getByText("no courses found")).toBeVisible();
});
