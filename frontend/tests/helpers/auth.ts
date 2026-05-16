import { type Page } from "@playwright/test";
import { User } from "@/tests/data";

export async function loginAs(page: Page, user: User) {
  await page.goto("/");
  await page.getByRole("button", { name: "Login" }).click();
  await page.getByRole("textbox", { name: "Username or email" }).fill(user.username);
  await page.getByRole("textbox", { name: "Username or email" }).press("Tab");
  await page.getByRole("textbox", { name: "Password" }).fill(user.password);
  await page.getByRole("button", { name: "Sign In" }).click();
  await page.waitForURL(/\/dashboard(?:\/.*)?$/);
}
