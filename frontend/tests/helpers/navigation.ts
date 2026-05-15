import { type Page } from "@playwright/test";

export async function clickNavbarButton(
  page: Page,
  buttonText: string,
  expectedUrl: string,
  buttonIndex = 0
) {
  const navbar = page.getByRole("navigation");
  await navbar.getByRole("link", { name: buttonText }).nth(buttonIndex).click();
  await page.waitForURL(expectedUrl);
}
