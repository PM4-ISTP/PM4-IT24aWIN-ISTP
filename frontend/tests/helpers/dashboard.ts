import { expect, type Page } from "@playwright/test";

export async function assertNoActiveLabs(page: Page) {
  await expect(page.getByText("No active labs", { exact: true })).toBeVisible();
}
