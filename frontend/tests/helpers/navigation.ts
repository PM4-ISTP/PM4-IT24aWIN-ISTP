import { type Locator, type Page } from "@playwright/test";

export async function clickButtonAndAssert(
  locateButton: () => Locator,
  assert: () => Promise<void>
) {
  await locateButton().click({ timeout: 5000 });
  await assert();
}

export async function clickButtonAndAssertUrl(
  page: Page,
  locateButton: () => Locator,
  expectedUrl: string
) {
  await clickButtonAndAssert(locateButton, async () => {
    await page.waitForURL(expectedUrl, { timeout: 6000 });
  });
}

export async function clickNavbarButton(
  page: Page,
  buttonText: string,
  expectedUrl: string,
  buttonIndex = 0
) {
  const navbar = page.getByRole("navigation");
  const buttonLocator = () => navbar.getByRole("link", { name: buttonText }).nth(buttonIndex);
  await clickButtonAndAssertUrl(page, buttonLocator, expectedUrl);
}
