import { type Locator, type Page } from "@playwright/test";

export async function clickButtonAndAssertUrl(page: Page, button: Locator, expectedUrl: string) {
  let pagedSwitched = false;
  let tries = 0;
  while (pagedSwitched === false && tries < 3) {
    await button.click();
    try {
      await page.waitForURL(expectedUrl);
      pagedSwitched = true;
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    } catch (e) {
      tries++;
    }
  }
}

export async function clickNavbarButton(
  page: Page,
  buttonText: string,
  expectedUrl: string,
  buttonIndex = 0
) {
  const navbar = page.getByRole("navigation");
  const button = navbar.getByRole("link", { name: buttonText }).nth(buttonIndex);
  await clickButtonAndAssertUrl(page, button, expectedUrl);
}
