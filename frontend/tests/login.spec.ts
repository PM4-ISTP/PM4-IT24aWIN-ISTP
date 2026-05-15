import test, { expect, type Page } from "@playwright/test";
import { loginAs, TestUser } from "@/tests/helpers";

async function logInAndThenLogOut(page: Page, user: TestUser, username: string, userRole: string) {
  const userMenuTriggerId = "user-menu-trigger";
  await loginAs(page, user);
  await expect(
    page.getByTestId(userMenuTriggerId).getByText(username, { exact: true })
  ).toBeVisible();
  await expect(
    page.getByTestId(userMenuTriggerId).getByText(userRole, { exact: true })
  ).toBeVisible();
  await page.getByTestId(userMenuTriggerId).click();
  await page.getByTestId("logout-link").click();
  await page.click("#kc-logout");
  await page.waitForURL("/");
  await expect(page.getByRole("button", { name: "Login" })).toBeVisible();
}

test("User can log in and then log out to log in as another user. The user menu trigger gets rendered correctly each time.", async ({
  page,
}) => {
  await logInAndThenLogOut(page, TestUser.Student, "E2E Student", "Student");
  await logInAndThenLogOut(page, TestUser.Instructor, "E2E Instructor", "Instructor");
  await logInAndThenLogOut(page, TestUser.Admin, "E2E Admin", "Admin");
});
