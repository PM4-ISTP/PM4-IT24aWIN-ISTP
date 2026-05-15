import test, { expect, type Page } from "@playwright/test";
import { clickNavbarButton } from "@/tests/helpers/navigation";
import adminCourse01 from "@/tests/files/courses/admin_01.json";
import instructorCourse01 from "@/tests/files/courses/instructor_01.json";
import instructorCourse04 from "@/tests/files/courses/instructor_04.json";
import { loginAs, TestUser } from "@/tests/helpers/auth";
import { assertCourseCards } from "@/tests/helpers/course";

const e2eTopicOptions = ["E2E-Testing-01", "E2E-Testing-02"];

async function clickTopicOption(page: Page, initialOption: string, desiredOption: string) {
  await expect(page.getByRole("combobox", { name: "Topic" })).toHaveValue(initialOption);
  await page.getByRole("combobox", { name: "Topic" }).click();
  for (let i = 0; i < e2eTopicOptions.length; i++) {
    await expect(page.getByRole("option", { name: e2eTopicOptions[i] })).toBeVisible();
  }

  await page.getByRole("option", { name: desiredOption }).click();
  await expect(page.getByRole("combobox", { name: "Topic" })).toHaveValue(desiredOption);
  for (let i = 0; i < e2eTopicOptions.length; i++) {
    await expect(page.getByRole("option", { name: e2eTopicOptions[i] })).not.toBeVisible();
  }

  await page.getByRole("button", { name: "Search" }).click();
  await expect(page.getByRole("combobox", { name: "Topic" })).toHaveValue(desiredOption);
}

test("Search and filter functionalities of catalog functions correctly", async ({ page }) => {
  await loginAs(page, TestUser.Student);
  await clickNavbarButton(page, "BROWSE / CATALOG", "dashboard/catalog");

  // Apply search query
  await page.getByRole("textbox", { name: "Search courses" }).fill("E2E");
  await page.getByRole("button", { name: "Search" }).click();
  await assertCourseCards(page, [instructorCourse04, adminCourse01, instructorCourse01]);
  await expect(page.getByRole("textbox", { name: "Search courses" })).toHaveValue("E2E");

  // Select topic with matching courses
  await clickTopicOption(page, "All topics", "E2E-Testing-01");
  await assertCourseCards(page, [adminCourse01, instructorCourse01]);
  await expect(page.getByRole("textbox", { name: "Search courses" })).toHaveValue("E2E");

  // Reset search
  await page.getByRole("button", { name: "Reset" }).click();
  await expect(page.getByRole("textbox", { name: "Search courses" })).toHaveValue("");
  await expect(page.getByRole("combobox", { name: "Topic" })).toHaveValue("E2E-Testing-01");

  // Select topic with no matching courses
  await clickTopicOption(page, "E2E-Testing-01", "E2E-Testing-02");
  await assertCourseCards(page, []);
  await expect(page.getByRole("textbox", { name: "Search courses" })).toHaveValue("");
});
