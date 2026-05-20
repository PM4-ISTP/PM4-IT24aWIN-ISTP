import { test } from "@/tests/fixtures";
import { expect, type Page } from "@playwright/test";
import {
  clickButtonAndAssert,
  clickButtonAndAssertUrl,
  clickNavbarButton,
} from "@/tests/helpers/navigation";
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
  const locateCourseCard = () =>
    page
      .getByRole("button")
      .filter({ has: page.getByText(course.title ?? "", { exact: true }) })
      .first();
  await expect(locateCourseCard()).toBeVisible();
  await clickButtonAndAssertUrl(page, locateCourseCard, `/dashboard/courses/${course.id}`);
}

async function openLabFromCourse(page: Page, course: Course, lab: Lab) {
  const locateActionButton = () => page.getByTestId("course-enrollment-action");
  await expect(locateActionButton()).toHaveText("Continue Course");
  await locateActionButton().click();
  await clickButtonAndAssertUrl(
    page,
    locateActionButton,
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
  expect(
    Math.abs((expiresAt?.getTime() ?? 0) - expectedExpiry.getTime()) / 60_000
  ).toBeLessThanOrEqual(TIME_TOLERANCE_MINUTES);
}

async function openActiveLab(page: Page, courseId: string, labId: string) {
  const url = `/dashboard/courses/${courseId}/labs/${labId}/play`;
  const activeLabLocator = () => page.getByTestId("active-lab-card").locator(`a[href="${url}"]`);
  await clickButtonAndAssertUrl(page, activeLabLocator, url);
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
  await expect(page.getByText("Running")).toBeVisible({ timeout: 120_000 });
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

test("Labs tab must be empty, if user has not created any labs.", async ({ page }) => {
  await loginAs(page, testUsers.instructorWithoutCoursesOrLabs);
  await clickNavbarButton(page, LAB_OVERVIEW_TAB_NAME, LAB_OVERVIEW_URL);
  await expect(page.getByText("no labs found")).toBeVisible();
});

test("Instructor can create a lab with one challenge.", async ({ page }) => {
  const getChallengeDescriptionField = () => {
    return page.getByRole("textbox").filter({ hasText: /^$/ }).nth(5);
  };

  await loginAs(page, testUsers.instructor);
  await clickNavbarButton(page, LAB_OVERVIEW_TAB_NAME, LAB_OVERVIEW_URL);
  await clickButtonAndAssertUrl(
    page,
    () => page.getByRole("link", { name: "New Lab" }),
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
  await page
    .getByRole("textbox", { name: "Title", exact: true })
    .fill("E2E Test Lab: Create Lab Test - Challenge 1");
  await getChallengeDescriptionField().click();
  await getChallengeDescriptionField().fill("This is the first challenge of this lab.");
  await page.getByRole("textbox", { name: "Flag" }).click();
  await page.getByRole("textbox", { name: "Flag" }).fill("FLAG");
  await clickButtonAndAssertUrl(
    page,
    () => page.getByRole("button", { name: "Create Lab" }),
    LAB_OVERVIEW_URL
  );

  // Verify lab created
  const labCard = page.getByRole("button", { name: "E2E Test Lab: Create Lab Test" });
  await expect(labCard).toBeVisible();
});

test("Instructor can update a lab with one challenge.", async ({ page }) => {
  const labUnderTest = labs.instructor01;
  const newDockerImage =
    "ghcr.io/pm4-istp/llm01-prompt-injection@sha256:c3fe94d1655076f00f522d923f4325b6bd232f40959b6fbda3d5f1e1a6edc70a";

  await loginAs(page, testUsers.instructor);
  await clickNavbarButton(page, LAB_OVERVIEW_TAB_NAME, LAB_OVERVIEW_URL);
  await clickButtonAndAssertUrl(
    page,
    () => page.getByRole("button", { name: labUnderTest.title }),
    `dashboard/instructor/labs/${labUnderTest.id}`
  );

  // Update lab
  await page.getByRole("textbox", { name: "Lab Title" }).click();
  await page.getByRole("textbox", { name: "Lab Title" }).fill("E2E Test Lab: Update Lab Test");
  await page.getByText(labUnderTest.description ?? "").dblclick();
  await page
    .getByRole("textbox")
    .filter({ hasText: labUnderTest.description ?? "" })
    .fill("This is a test for lab update.");
  await page.getByRole("textbox", { name: "Docker Image" }).click();
  await page.getByRole("textbox", { name: "Docker Image" }).fill(newDockerImage);
  await expect(page.getByText("Public GHCR image found")).toBeVisible();
  await page.locator("label").filter({ hasText: "Private" }).click();
  await page.locator("label").filter({ hasText: "Expert" }).click();
  await clickButtonAndAssertUrl(
    page,
    () => page.getByRole("button", { name: "Save Changes" }),
    LAB_OVERVIEW_URL
  );

  // Verify lab updated
  const labCard = page.getByRole("button", { name: "E2E Test Lab: Update Lab Test" });
  await expect(labCard).toBeVisible();
});

test("Instructor can delete a lab using the edit lab view.", async ({ page }) => {
  const labUnderTest = labs.instructor01;

  // Delete lab
  await loginAs(page, testUsers.instructor);
  await clickNavbarButton(page, LAB_OVERVIEW_TAB_NAME, LAB_OVERVIEW_URL);
  await clickButtonAndAssertUrl(
    page,
    () => page.getByRole("button", { name: labUnderTest.title }),
    `dashboard/instructor/labs/${labUnderTest.id}`
  );
  await clickButtonAndAssert(
    () => page.getByRole("button", { name: "Delete Lab" }),
    async () => await expect(page.getByRole("dialog", { name: "Delete Lab" })).toBeVisible()
  );
  await clickButtonAndAssertUrl(
    page,
    () => page.getByLabel("Delete Lab").getByRole("button", { name: "Delete Lab" }),
    "dashboard/instructor/labs"
  );

  // Verify lab delete
  const labCard = page.getByRole("button", { name: "E2E Test Lab: Create Lab Test" });
  await expect(labCard).not.toBeVisible();
});

test("Admin can edit a lab using the admin dashboard.", async ({ page }) => {
  const labUnderTest = labs.instructor01;
  const newTitle = "E2E Test Lab: Update Lab Test";
  const getDescriptionInput = () =>
    page.getByRole("textbox").filter({ hasText: labUnderTest.description ?? "" });

  // Edit lab
  await loginAs(page, testUsers.admin);
  await clickNavbarButton(page, "Dashboard", "dashboard/admin", 1);
  await page.getByRole("tab", { name: "Labs" }).click();
  await page
    .getByRole("row", { name: labUnderTest.title })
    .getByRole("button", { name: "Edit lab" })
    .click();
  await page.getByRole("textbox", { name: "Title" }).click();
  await page.getByRole("textbox", { name: "Title" }).press("ControlOrMeta+a");
  await page.getByRole("textbox", { name: "Title" }).fill(newTitle);
  await expect(page.getByRole("textbox", { name: "Docker Image" })).toHaveAttribute("readonly");
  await expect(page.getByRole("textbox", { name: "Docker Image" })).toHaveValue(
    labUnderTest.dockerImage
  );
  await getDescriptionInput().click();
  await getDescriptionInput().press("ControlOrMeta+a");
  await getDescriptionInput().fill("This is a test for lab update.");

  await page.getByRole("combobox", { name: "Status" }).click();
  await page.getByRole("option", { name: "PRIVATE" }).click();
  await page.getByText("Difficulty").click();
  await page.getByRole("option", { name: "EASY" }).click();
  await page.getByRole("button", { name: "Save" }).click();

  // Verify lab edit
  const labRow = page.getByRole("row", { name: newTitle });
  await expect(labRow).toBeVisible();
});

test("Admin can delete a lab using the admin dashboard.", async ({ page }) => {
  const labUnderTest = labs.instructor01;

  // Delete lab
  await loginAs(page, testUsers.admin);
  await clickNavbarButton(page, "Dashboard", "dashboard/admin", 1);
  await page.getByRole("tab", { name: "Labs" }).click();
  await page
    .getByRole("row", { name: labUnderTest.title })
    .getByRole("button", { name: "Delete lab" })
    .click();
  await page.getByRole("button", { name: "Delete", exact: true }).click();

  // Verify lab delete
  const labRow = page.getByRole("row", { name: labUnderTest.title });
  await expect(labRow).not.toBeVisible();
});

test("Lab pod lifecycle for e2e-student", async ({ page }) => {
  test.setTimeout(300_000);
  let labRunning = false;

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
    labRunning = true;
    await openAppAndAssert(page);

    let expectedExpiry = addMinutes(new Date(), START_DURATION_MINUTES);
    await assertPodExpiry(page, expectedExpiry);

    expectedExpiry = addMinutes(expectedExpiry, EXTENSION_MINUTES);
    await extendLab(page, expectedExpiry);

    await clickNavbarButton(page, "HOME", "dashboard");
    await assertActiveLabCard(page, courseUnderTest, labUnderTest, expectedExpiry);
    await openActiveLab(page, courseUnderTest.id, labUnderTest.id);

    expectedExpiry = addMinutes(expectedExpiry, EXTENSION_MINUTES);
    await extendLab(page, expectedExpiry);
    await expect(extendButton).toBeDisabled();

    await clickNavbarButton(page, "HOME", "dashboard");
    await assertActiveLabCard(page, courseUnderTest, labUnderTest, expectedExpiry);

    await openActiveLab(page, courseUnderTest.id, labUnderTest.id);
    await stopLabAndWaitForNotStarted(page);
    labRunning = false;
    await assertAppNotReady(page);

    await clickNavbarButton(page, "HOME", "dashboard");
    await assertNoActiveLabs(page);

    await openCourseFromMyCourses(page, courseUnderTest);
    await openLabFromCourse(page, courseUnderTest, labUnderTest);
    await assertAppNotReady(page);

    await startLabAndWaitForRunning(page);
    labRunning = true;
    await openAppAndAssert(page);

    expectedExpiry = addMinutes(new Date(), START_DURATION_MINUTES);
    await assertPodExpiry(page, expectedExpiry);

    await clickNavbarButton(page, "HOME", "dashboard");
    await assertActiveLabCard(page, courseUnderTest, labUnderTest, expectedExpiry);

    await openActiveLab(page, courseUnderTest.id, labUnderTest.id);
    await stopLabAndWaitForNotStarted(page);
    labRunning = false;
    await assertAppNotReady(page);
  } finally {
    if (labRunning) {
      await clickNavbarButton(page, "HOME", "dashboard");
      await openActiveLab(page, courseUnderTest.id, labUnderTest.id);
      await stopLabAndWaitForNotStarted(page);
    }
  }
});
