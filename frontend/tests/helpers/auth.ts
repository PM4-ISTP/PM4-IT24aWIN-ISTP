import { type Page } from "@playwright/test";

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
