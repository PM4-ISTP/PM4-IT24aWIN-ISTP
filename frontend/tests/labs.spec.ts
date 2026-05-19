import test, { expect, type Page } from "@playwright/test";
import { clickButtonAndAssertUrl, clickNavbarButton } from "@/tests/helpers/navigation";
import { loginAs } from "@/tests/helpers/auth";
import { courses, defaultDockerImage, labs, testUsers, type Course, type Lab } from "@/tests/data";
import { assertNoActiveLabs } from "@/tests/helpers/dashboard";

const student = testUsers.student;
const courseUnderTest = courses.instructor01;
const labUnderTest = labs.instructor01;

const START_DURATION_MINUTES = 60;
const EXTENSION_MINUTES = 30;
const TIME_TOLERANCE_MINUTES = 1;

const LAB_OVERVIEW_TAB_NAME = "Labs";
const LAB_OVERVIEW_URL = "dashboard/instructor/labs";

function addMinutes(base: Date, minutes: number): Date {
  const next = new Date(base.getTime());
  next.setMinutes(next.getMinutes() + minutes);
  return next;
}

function parseDurationMinutes(label: string): number | null {
  const hoursMatch = label.match(/(\d+)\s*h/);
  const minutesMatch = label.match(/(\d+)\s*min/);
  if (!hoursMatch && !minutesMatch) return null;

  let minutes = 0;
  if (hoursMatch) minutes += Number(hoursMatch[1]) * 60;
  if (minutesMatch) minutes += Number(minutesMatch[1]);

  return Number.isNaN(minutes) ? null : minutes;
}

function parseTimeOnDate(label: string, referenceDate: Date): Date | null {
  const match = label.match(/(\d{2}):(\d{2})/);
  if (!match) return null;
  const parsed = new Date(referenceDate.getTime());
  parsed.setHours(Number(match[1]), Number(match[2]), 0, 0);
  return parsed;
}

function parseDashboardExpiry(label: string): Date | null {
  const match = label.match(/Expires\s+(\d{2})\.(\d{2})\.(\d{4}),\s*(\d{2}):(\d{2})/);
  if (!match) return null;
  const [, day, month, year, hour, minute] = match;
  const parsed = new Date(
    Number(year),
    Number(month) - 1,
    Number(day),
    Number(hour),
    Number(minute)
  );
  return Number.isNaN(parsed.getTime()) ? null : parsed;
}

async function openCourseFromMyCourses(page: Page, course: Course) {
  await clickNavbarButton(page, "MY COURSES", "dashboard/courses");
  const courseCard = page
    .getByRole("button")
    .filter({ has: page.getByText(course.title ?? "", { exact: true }) })
    .first();
  await expect(courseCard).toBeVisible();
  await clickButtonAndAssertUrl(page, courseCard, `/dashboard/courses/${course.id}`);
}

async function openLabFromCourse(page: Page, course: Course, lab: Lab) {
  const actionButton = page.getByTestId("course-enrollment-action");
  await expect(actionButton).toHaveText("Continue Course");
  await actionButton.click();
  await clickButtonAndAssertUrl(
    page,
    actionButton,
    `/dashboard/courses/${course.id}/labs/${lab.id}/play`
  );
  await expect(page.getByRole("heading", { name: lab.title, level: 2 })).toBeVisible();
}

async function readPodExpiry(page: Page, referenceDate: Date) {
  const expiresInLabel = page.getByText(/^Expires in /).first();
  await expect(expiresInLabel).toBeVisible();
  const expiresInText = await expiresInLabel.innerText();
  const expiresInMinutes = parseDurationMinutes(expiresInText);
  expect(expiresInMinutes).not.toBeNull();

  const expiresAtLabel = page.getByText(/^at \d{2}:\d{2}$/).first();
  await expect(expiresAtLabel).toBeVisible();
  const expiresAtText = await expiresAtLabel.innerText();
  const expiresAt = parseTimeOnDate(expiresAtText, referenceDate);
  expect(expiresAt).not.toBeNull();

  return { expiresInMinutes: expiresInMinutes ?? 0, expiresAt: expiresAt ?? referenceDate };
}

async function assertPodExpiry(page: Page, expectedExpiry: Date) {
  const { expiresInMinutes, expiresAt } = await readPodExpiry(page, expectedExpiry);
  const expectedMinutes = Math.round((expectedExpiry.getTime() - Date.now()) / 60_000);

  expect(Math.abs(expiresInMinutes - expectedMinutes)).toBeLessThanOrEqual(TIME_TOLERANCE_MINUTES);
  expect(Math.abs(expiresAt.getTime() - expectedExpiry.getTime()) / 60_000).toBeLessThanOrEqual(
    TIME_TOLERANCE_MINUTES
  );
}

async function assertActiveLabCard(page: Page, course: Course, lab: Lab, expectedExpiry: Date) {
  const activeLabCard = page.getByTestId("active-lab-card");

  const expectedHref = `/dashboard/courses/${course.id}/labs/${lab.id}/play`;
  await expect(activeLabCard.locator(`a[href="${expectedHref}"]`)).toBeVisible();
  await expect(activeLabCard.getByText(lab.title, { exact: true })).toBeVisible();
  await expect(activeLabCard.getByText(course.title, { exact: true })).toBeVisible();

  const expiresText = await activeLabCard
    .getByText(/^Expires /)
    .first()
    .innerText();
  const expiresAt = parseDashboardExpiry(expiresText);
  expect(expiresAt).not.toBeNull();
  console.log("Actual expiry: ", expiresAt);
  console.log("Expected expiry: ", expectedExpiry);
  expect(
    Math.abs((expiresAt?.getTime() ?? 0) - expectedExpiry.getTime()) / 60_000
  ).toBeLessThanOrEqual(TIME_TOLERANCE_MINUTES);
}

async function openAppAndAssert(page: Page) {
  const openAppLink = page.getByRole("link", { name: "Open app" });
  await expect(openAppLink).toBeVisible();
  await expect(openAppLink).toHaveAttribute("href", /http/);

  const [appPage] = await Promise.all([page.context().waitForEvent("page"), openAppLink.click()]);

  await appPage.waitForLoadState("domcontentloaded");
  await expect(appPage.getByRole("heading", { name: "Campus Helpdesk", level: 1 })).toBeVisible();
  await appPage.close();
}

async function startLabAndWaitForRunning(page: Page) {
  await page.getByLabel("Start lab").click();
  await expect(page.getByText(/^Running$/)).toBeVisible({ timeout: 120_000 });
}

async function stopLabAndWaitForNotStarted(page: Page) {
  page.once("dialog", (dialog) => dialog.accept());
  await page.getByLabel("Stop lab").click();
  await expect(page.getByText(/^Not started$/)).toBeVisible({ timeout: 120_000 });
}

async function extendLab(page: Page, expectedExpiry: Date) {
  const extendButton = page.getByLabel("Extend lab");
  await expect(extendButton).toBeEnabled();
  const expiresInLabel = page.getByText(/^Expires in /).first();
  const previousExpiryLabel = await expiresInLabel.innerText();
  await extendButton.click();
  await expect(expiresInLabel).not.toHaveText(previousExpiryLabel);
  await assertPodExpiry(page, expectedExpiry);
}

async function assertAppNotReady(page: Page) {
  const appNotReady = page.getByTestId("open-app-button");
  await expect(appNotReady).toBeVisible();
  await expect(appNotReady).toHaveAttribute("data-disabled", "true"); // Mantine uses data-disabled instead of disabled for disabling button
}

function getChallengeDescriptionField(page: Page) {
  return page.getByRole("textbox").filter({ hasText: /^$/ }).nth(5)
}

test("Labs tab must be empty, if user has not created any labs.", async ({ page }) => {
  await loginAs(page, testUsers.instructorWithoutCoursesOrLabs);
  await clickNavbarButton(page, LAB_OVERVIEW_TAB_NAME, LAB_OVERVIEW_URL);
  await expect(page.getByText("no labs found")).toBeVisible();
});

test("Instructor can create a lab with one challenge.", async ({ page }) => {
  await loginAs(page, testUsers.instructor);
  await clickNavbarButton(page, LAB_OVERVIEW_TAB_NAME, LAB_OVERVIEW_URL);
  await clickButtonAndAssertUrl(
    page,
    page.getByRole("link", { name: "New Lab" }),
    "dashboard/instructor/labs/create"
  );

  // Create lab
  await page.getByRole("textbox", { name: "Lab Title" }).click();
  await page.getByRole("textbox", { name: "Lab Title" }).fill("E2E Test Lab: Create Lab Test");
  await page.getByText("Add a description...").dblclick();
  await page
    .getByRole("textbox")
    .filter({ hasText: "Add a description..." })
    .fill("This is a test for lab creation.");
  await page.getByRole("textbox", { name: "Docker Image" }).click();
  await page.getByRole("textbox", { name: "Docker Image" }).fill(defaultDockerImage);
  await expect(page.getByText("Public GHCR image found")).toBeVisible();
  await page.locator("label").filter({ hasText: "Public" }).click();
  await page.locator("label").filter({ hasText: "Beginner" }).click();
  await page.getByRole("textbox", { name: "Title", exact: true }).click();
  await page.getByRole("textbox", { name: "Title", exact: true }).fill("Challenge 1");
  await page.getByRole("textbox").filter({ hasText: /^$/ }).nth(5).click();
  await getChallengeDescriptionField(page).fill("This is the first challenge of this lab.");
  await page.getByRole("textbox", { name: "Flag" }).click();
  await page.getByRole("textbox", { name: "Flag" }).fill("FLAG");
  await clickButtonAndAssertUrl(
    page,
    page.getByRole("button", { name: "Create Lab" }),
    LAB_OVERVIEW_URL
  );

  // Verify lab created
  const labCard = page.getByRole("button", { name: "E2E Test Lab: Create Lab Test" });
  await expect(labCard).toBeVisible();
});

test("Lab pod lifecycle for e2e-student", async ({ page }) => {
  test.setTimeout(300_000);

  try {
    await loginAs(page, student);

    await clickNavbarButton(page, "HOME", "dashboard");
    await assertNoActiveLabs(page);

    await openCourseFromMyCourses(page, courseUnderTest);
    await openLabFromCourse(page, courseUnderTest, labUnderTest);
    await assertAppNotReady(page);

    const extendButton = page.getByLabel("Extend lab");
    await expect(extendButton).toBeDisabled();
    await extendButton.hover();
    await expect(page.getByText("Only running labs can be extended")).toBeVisible();

    await startLabAndWaitForRunning(page);
    await openAppAndAssert(page);

    let expectedExpiry = addMinutes(new Date(), START_DURATION_MINUTES);
    await assertPodExpiry(page, expectedExpiry);

    expectedExpiry = addMinutes(expectedExpiry, EXTENSION_MINUTES);
    await extendLab(page, expectedExpiry);

    await clickNavbarButton(page, "HOME", "dashboard");
    await assertActiveLabCard(page, courseUnderTest, labUnderTest, expectedExpiry);

    await page
      .getByTestId("active-lab-card")
      .locator(`a[href="/dashboard/courses/${courseUnderTest.id}/labs/${labUnderTest.id}/play"]`)
      .click();

    expectedExpiry = addMinutes(expectedExpiry, EXTENSION_MINUTES);
    await extendLab(page, expectedExpiry);
    await expect(extendButton).toBeDisabled();

    await clickNavbarButton(page, "HOME", "dashboard");
    await assertActiveLabCard(page, courseUnderTest, labUnderTest, expectedExpiry);

    await page
      .getByTestId("active-lab-card")
      .locator(`a[href="/dashboard/courses/${courseUnderTest.id}/labs/${labUnderTest.id}/play"]`)
      .click();
    await stopLabAndWaitForNotStarted(page);
    await assertAppNotReady(page);

    await clickNavbarButton(page, "HOME", "dashboard");
    await assertNoActiveLabs(page);

    await openCourseFromMyCourses(page, courseUnderTest);
    await openLabFromCourse(page, courseUnderTest, labUnderTest);
    await assertAppNotReady(page);

    await startLabAndWaitForRunning(page);
    await openAppAndAssert(page);

    expectedExpiry = addMinutes(new Date(), START_DURATION_MINUTES);
    await assertPodExpiry(page, expectedExpiry);

    await clickNavbarButton(page, "HOME", "dashboard");
    await assertActiveLabCard(page, courseUnderTest, labUnderTest, expectedExpiry);

    await page
      .getByTestId("active-lab-card")
      .locator(`a[href="/dashboard/courses/${courseUnderTest.id}/labs/${labUnderTest.id}/play"]`)
      .click();
    await stopLabAndWaitForNotStarted(page);
    await assertAppNotReady(page);
  } finally {
    // TODO: add clean up for failed test
  }
});
