import { type Locator, type Page } from "@playwright/test";

export async function clickButtonAndAssert(
  locateButton: () => Locator,
  assertion: () => Promise<void>
) {
  let clickSuccessful = false;
  let tries = 0;
  while (clickSuccessful === false && tries < 5) {
    await locateButton().click();
    try {
      await assertion();
      clickSuccessful = true;
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
    } catch (e) {
      tries++;
    }
  }
}

export async function clickButtonAndAssertUrl(
  page: Page,
  locateButton: () => Locator,
  expectedUrl: string
) {
  await clickButtonAndAssert(locateButton, async () => {
    await page.waitForURL(expectedUrl);
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
