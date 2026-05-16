import test, { expect } from "@playwright/test";
import { clickNavbarButton } from "@/tests/helpers/navigation";
import { loginAs } from "@/tests/helpers/auth";
import { testUsers } from "@/tests/data";

test("Labs tab must be empty, if user has not created any labs.", async ({ page }) => {
  await loginAs(page, testUsers.instructorWithoutCoursesOrLabs);
  await clickNavbarButton(page, "Labs", "dashboard/instructor/labs");
  await expect(page.getByText("no labs found")).toBeVisible();
});
