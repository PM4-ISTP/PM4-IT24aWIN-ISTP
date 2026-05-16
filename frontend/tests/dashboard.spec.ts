import test, { expect, type Page } from "@playwright/test";
import { assertCourseCards, type Course } from "@/tests/helpers/course";
import { loginAs, TestUser } from "@/tests/helpers/auth";
import {
  dashboardTestData,
  adminCourse01,
  instructorCourse01,
  instructorCourse02,
  instructorCourse04,
} from "@/tests/data";

type Deadline = {
  courseId: string;
  courseTitle: string;
  labId: string;
  labTitle: string;
  dueAt: string;
};

type RenderDashboardTestParameters = {
  roleName: string;
  user: TestUser;
  dataKey: keyof typeof dashboardTestData;
  displayedCourses: Course[];
};

function formatDeadlineDate(dueAt: string | number): string {
  return new Date(dueAt).toLocaleString("de-CH", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

function getHeroStatisticValue(page: Page, label: string) {
  return page.getByText(label, { exact: true }).first().locator("xpath=following-sibling::*[1]");
}

async function assertUpcomingDeadlines(page: Page, visibleDeadlines: readonly Deadline[]) {
  const now = Date.now();
  const overdue = visibleDeadlines
    .filter((item) => new Date(item.dueAt).getTime() < now)
    .sort((a, b) => new Date(a.dueAt).getTime() - new Date(b.dueAt).getTime());
  const due = visibleDeadlines
    .filter((item) => new Date(item.dueAt).getTime() >= now)
    .sort((a, b) => new Date(a.dueAt).getTime() - new Date(b.dueAt).getTime());
  const expectedOrder = [...overdue, ...due];

  if (expectedOrder.length === 0) {
    await expect(
      page.getByText("No deadlines set for your enrolled courses.", { exact: true })
    ).toBeVisible();
    await expect(page.locator('[title="Aus Kalender entfernen"]')).toHaveCount(0);
    return;
  }

  const expectedHrefs = expectedOrder.map(
    (item) =>
      `/dashboard/courses/${encodeURIComponent(item.courseId)}/labs/${encodeURIComponent(item.labId)}/play`
  );

  const actualHrefs = await page
    .locator('a[href*="/dashboard/courses/"][href*="/labs/"][href$="/play"]')
    .evaluateAll((links) => links.map((link) => link.getAttribute("href") ?? ""));
  expect(actualHrefs).toEqual(expectedHrefs);

  for (const item of expectedOrder) {
    const href = `/dashboard/courses/${encodeURIComponent(item.courseId)}/labs/${encodeURIComponent(item.labId)}/play`;
    const rowLink = page.locator(`a[href="${href}"]`).first();
    const row = rowLink.locator(
      "xpath=ancestor::div[.//span[normalize-space()='OVERDUE' or normalize-space()='DUE']][1]"
    );

    await expect(rowLink).toBeVisible();
    await expect(rowLink).toHaveText(item.labTitle);
    await expect(rowLink).toHaveAttribute("href", href);
    await expect(row).toContainText(item.courseTitle);

    const isOverdue = new Date(item.dueAt).getTime() < now;
    await expect(row.getByText(isOverdue ? "OVERDUE" : "DUE", { exact: true })).toBeVisible();
    await expect(row).toContainText(formatDeadlineDate(item.dueAt));

    const dismissIcon = row.locator('[title="Aus Kalender entfernen"]');
    await expect(dismissIcon).toHaveCount(isOverdue ? 1 : 0);
  }

  await expect(page.locator('[title="Aus Kalender entfernen"]')).toHaveCount(overdue.length);
}

const DASHBOARD_ROLE_CASES: Array<RenderDashboardTestParameters> = [
  {
    roleName: "Instructor",
    user: TestUser.Instructor,
    dataKey: "instructor",
    displayedCourses: [instructorCourse02, instructorCourse04, instructorCourse01],
  },
  { roleName: "Admin", user: TestUser.Admin, dataKey: "admin", displayedCourses: [adminCourse01] },
  {
    roleName: "Student",
    user: TestUser.Student,
    dataKey: "student",
    displayedCourses: [instructorCourse02, instructorCourse04, adminCourse01],
  },
  {
    roleName: "Instructor (without courses or labs)",
    user: TestUser.InstructorWithoutCoursesOrLabs,
    dataKey: "instructorWithoutCoursesOrLabs",
    displayedCourses: [],
  },
];

test.describe("Dashboard widgets for all primary roles and a user without courses and deadlines", () => {
  // Group: End-to-end dashboard checks (cards, deadlines, hero stats, and empty active labs) per role.
  for (const { roleName, user, dataKey, displayedCourses } of DASHBOARD_ROLE_CASES) {
    test(`Dashboard data for ${roleName} matches expected test data and ordering rules`, async ({
      page,
    }) => {
      await loginAs(page, user);

      const expectedData = dashboardTestData[dataKey];
      const deadlines = expectedData.upcomingDeadlines;

      await assertCourseCards(page, displayedCourses);
      await assertUpcomingDeadlines(page, deadlines);

      await expect(getHeroStatisticValue(page, "Enrolled Courses")).toHaveText(
        String(expectedData.enrolledCoursesCount)
      );
      await expect(getHeroStatisticValue(page, "Completed Labs")).toHaveText(
        String(expectedData.completedLabsCount)
      );

      // Verify no active labs
      await expect(page.getByText("No active labs", { exact: true })).toBeVisible();
    });
  }
});
