import test, { expect, type Page } from "@playwright/test";
import { loginAs, TestUser } from "@/tests/helpers";

function getHeroStatisticElement(page: Page, name: string, value: number) {
  const query = name + value;
  return page.getByText(query);
}

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
