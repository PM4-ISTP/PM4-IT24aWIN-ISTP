import { type Page, type Response, expect } from "@playwright/test";

export enum TestUser {
  Admin,
  Instructor,
  InstructorWithoutCoursesOrLabs,
  Student,
}

interface LoginCredentials {
  username: string;
  password: string;
}

export type Course = {
  id?: string;
  title?: string;
  isPrivate?: boolean;
  isPublished?: boolean;
  topic?: string | null;
  ownerName?: string | null;
  ownerTitle?: string | null;
  shortDescription?: string | null;
  description?: string | null;
  updatedAt?: string | null;
};

function getLoginCredentialsForUser(user: TestUser): LoginCredentials {
  switch (user) {
    case TestUser.Admin:
      return {
        username: process.env.E2E_ADMIN_USERNAME ?? "e2e-admin",
        password: process.env.E2E_ADMIN_PASSWORD ?? "e2e-admin",
      };
    case TestUser.Instructor:
      return {
        username: process.env.E2E_INSTRUCTOR_USERNAME ?? "e2e-instructor",
        password: process.env.E2E_INSTRUCTOR_PASSWORD ?? "e2e-instructor",
      };
    case TestUser.InstructorWithoutCoursesOrLabs:
      return {
        username:
          process.env.E2E_INSTRUCTOR_WITHOUT_COURSES_OR_LABS_USERNAME ??
          "e2e-instructor-without-courses-or-labs",
        password:
          process.env.E2E_INSTRUCTOR_WITHOUT_COURSES_OR_LABS_PASSWORD ??
          "e2e-instructor-without-courses-or-labs",
      };
    case TestUser.Student:
      return {
        username: process.env.E2E_STUDENT_USERNAME ?? "e2e-student",
        password: process.env.E2E_STUDENT_PASSWORD ?? "e2e-student",
      };
    default:
      throw Error("The given test user does not exist.");
  }
}

export async function loginAs(page: Page, user: TestUser) {
  const loginCredentials = getLoginCredentialsForUser(user);
  await page.goto("/");
  await page.getByRole("button", { name: "Login" }).click();
  await page.getByRole("textbox", { name: "Username or email" }).fill(loginCredentials.username);
  await page.getByRole("textbox", { name: "Username or email" }).press("Tab");
  await page.getByRole("textbox", { name: "Password" }).fill(loginCredentials.password);
  await page.getByRole("button", { name: "Sign In" }).click();
  await page.waitForURL(/\/dashboard(?:\/.*)?$/);
}

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

function escapeRegExp(value: string): string {
  return value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

function getCourseStatusLabel(course: Course): "Private" | "Published" | "Draft" {
  if (course.isPrivate) return "Private";
  if (course.isPublished) return "Published";
  return "Draft";
}

function formatDate(date?: string | null | number): string {
  if (!date) {
    return "No date specified";
  }

  return new Date(typeof date === "number" ? date : date).toLocaleDateString("de-CH", {
    day: "numeric",
    month: "short",
    year: "numeric",
  });
}

export async function assertCourseCards(page: Page, courses: Course[]) {
  const courseCards = page
    .getByRole("button")
    .filter({ has: page.getByText(/^(Private|Published|Draft)$/) });

  if (courses.length === 0) {
    await expect(page.getByText("No courses found", { exact: true })).toBeVisible();
    await expect(courseCards).toHaveCount(0);
    return;
  }

  await expect(courseCards).toHaveCount(courses.length);

  const expectedTitles = courses
    .map((course) => course.title ?? "")
    .filter((title) => title.length > 0);
  const titleOrderInUi = await courseCards.evaluateAll((buttons, titles) => {
    const normalize = (value: string) => value.toLowerCase().replace(/\s+/g, " ").trim();
    const normalizedTitles = titles.map((title) => ({ raw: title, normalized: normalize(title) }));
    const found: string[] = [];

    for (const button of buttons) {
      const text = normalize(button.textContent ?? "");
      const match = normalizedTitles.find((title) => text.includes(title.normalized));
      if (match) {
        found.push(match.raw);
      }
    }

    return found;
  }, expectedTitles);
  expect(titleOrderInUi).toEqual(expectedTitles);

  for (const course of courses) {
    const title = course.title ?? "";
    const card = courseCards.filter({ has: page.getByText(title, { exact: true }) }).first();

    await expect(card, `Course card for "${title}" should be visible`).toBeVisible();
    await expect(card.getByText(getCourseStatusLabel(course), { exact: true })).toBeVisible();
    await expect(card.getByText(title, { exact: true })).toBeVisible();

    if (course.topic) {
      await expect(
        card.getByText(new RegExp(`^${escapeRegExp(course.topic)}$`, "i"))
      ).toBeVisible();
    }

    if (course.ownerName) {
      await expect(card.getByText(course.ownerName, { exact: true })).toBeVisible();
      await expect(card.getByText(course.ownerTitle ?? "", { exact: true })).toBeVisible();
    }

    await expect(card).toContainText(course.shortDescription ?? "");
    await expect(card).toContainText(formatDate(course.updatedAt));
  }
}

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
