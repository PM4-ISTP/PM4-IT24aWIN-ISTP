import test, { expect } from "@playwright/test";
import { clickNavbarButton, loginAs, TestUser } from "@/tests/helpers";

test("Labs tab must be empty, if user has not created any labs.", async ({ page }) => {
  await loginAs(page, TestUser.InstructorWithoutCoursesOrLabs);
  await clickNavbarButton(page, "Labs", "dashboard/instructor/labs");
  await expect(page.getByText("no labs found")).toBeVisible();
});
