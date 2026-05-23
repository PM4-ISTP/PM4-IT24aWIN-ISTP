import { test } from "@/tests/fixtures";
import { expect, type Page } from "@playwright/test";
import { loginAs } from "@/tests/helpers/auth";
import { testUsers, type User } from "@/tests/data";
import { clickButtonAndAssertUrl } from "@/tests/helpers/navigation";

async function logInAndThenLogOut(page: Page, user: User) {
  const userMenuTriggerId = "user-menu-trigger";
  await loginAs(page, user);
  await expect(
    page.getByTestId(userMenuTriggerId).getByText(user.name, { exact: true })
  ).toBeVisible();
  await expect(
    page.getByTestId(userMenuTriggerId).getByText(user.role, { exact: true })
  ).toBeVisible();
  await page.getByTestId(userMenuTriggerId).click();
  await page.getByTestId("logout-link").click();
  await clickButtonAndAssertUrl(page, () => page.locator("#kc-logout"), "/");
  await expect(page.getByRole("button", { name: "Login" })).toBeVisible();
}

test("User can log in and then log out to log in as another user. The user menu trigger gets rendered correctly each time.", async ({
  page,
}) => {
  test.setTimeout(90_000);
  await logInAndThenLogOut(page, testUsers.student);
  await logInAndThenLogOut(page, testUsers.instructor);
  await logInAndThenLogOut(page, testUsers.admin);
});
