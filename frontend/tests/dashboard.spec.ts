import test, { expect, type Page } from "@playwright/test";
import { loginAs, TestUser } from "@/tests/helpers";

async function logInAndThenLogOut(page: Page, user: TestUser, username: string, userRole: string) {
  const userMenuTriggerId = "user-menu-trigger";
  await loginAs(page, user);
  await expect(page.getByTestId(userMenuTriggerId).getByText(username, { exact: true })).toBeVisible();
  await expect(page.getByTestId(userMenuTriggerId).getByText(userRole, { exact: true })).toBeVisible();
  await page.getByTestId(userMenuTriggerId).click();
  await page.getByTestId("logout-link").click();
  await page.click("#kc-logout");
  await page.waitForURL("/");
  await expect(page.getByRole("button", { name: "Login" })).toBeVisible();
}

function getHeroStatisticElement(page: Page, name: string, value: number) {
  const query = name + value;
  return page.getByText(query);
}

test("User can log in and then log out to log in as another user. The user menu trigger gets rendered correctly each time.", async ({ page }) => {
  await logInAndThenLogOut(page, TestUser.Student, "E2E Student", "Student");
  await logInAndThenLogOut(page, TestUser.Instructor, "E2E Instructor", "Instructor");
  await logInAndThenLogOut(page, TestUser.Admin, "E2E Admin", "Admin");
});

test("Dashboard shows empty state for user without courses or labs", async ({ page }) => {
  await loginAs(page, TestUser.InstructorWithoutCoursesOrLabs);

  // Verify elements on dashboard
  await expect(page.getByText("no courses found")).toBeVisible();
  await expect(getHeroStatisticElement(page, "enrolled courses", 0)).toBeVisible();
  await expect(page.getByText("no deadlines set for your enrolled courses")).toBeVisible();
  await expect(page.getByText("no active labs")).toBeVisible();
  await expect(getHeroStatisticElement(page, "completed labs", 0)).toBeVisible();

  // Verify "My Badges" to be empty
  await page.getByRole("button", { name: "My Badges" }).click();
  const modal = page.getByRole("dialog");
  await expect(modal).toBeVisible();
  await expect(page.getByText("No badges yet")).toBeVisible();
  await modal.getByRole("button").click();
  await expect(modal).toBeHidden();
  await expect(page.getByText("No badges yet")).toBeHidden();
});
