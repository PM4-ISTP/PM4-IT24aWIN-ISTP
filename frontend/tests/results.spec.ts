import test, { expect } from "@playwright/test";
import { clickNavbarButton } from "@/tests/helpers/navigation";
import { loginAs, TestUser } from "@/tests/helpers/auth";

test("Results tab must be empty, if user is not an instructor of any courses.", async ({
  page,
}) => {
  await loginAs(page, TestUser.InstructorWithoutCoursesOrLabs);
  await clickNavbarButton(page, "Results", "dashboard/instructor/results");
  await expect(page.getByText("no courses found")).toBeVisible();
});
