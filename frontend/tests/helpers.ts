import { type Page, type Response, expect } from "@playwright/test";

export async function expectApiSuccess(
  page: Page,
  trigger: () => Promise<void>,
  urlPattern: string | RegExp,
  method?: string
) {
  const matcher = method
    ? (response: Response) =>
        !!response.url().match(urlPattern) && response.request().method() === method.toUpperCase()
    : urlPattern;
  const [response] = await Promise.all([page.waitForResponse(matcher), trigger()]);
  expect(response.ok()).toBeTruthy();
}
