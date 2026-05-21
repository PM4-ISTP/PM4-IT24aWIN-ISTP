import { test } from "@/tests/fixtures";
import { expect, type Page } from "@playwright/test";
import { assertCourseCards } from "@/tests/helpers/course";
import { loginAs } from "@/tests/helpers/auth";
import { type Course, testUsers, type Lab } from "@/tests/data";
import { formatDateTime } from "@/tests/helpers/date";
import { assertNoActiveLabs } from "@/tests/helpers/dashboard";

type Deadline = {
  courseId: string;
  courseTitle: string;
  labId: string;
  labTitle: string;
  dueAt: string;
};

function extractDeadlines(courses: readonly Course[], completedLabs: readonly Lab[]): Deadline[] {
  const deadlines: Deadline[] = [];
  for (const course of courses) {
    for (const assignment of course.labs) {
      if (assignment.dueAt && !completedLabs.includes(assignment.lab)) {
        deadlines.push({
          courseId: course.id,
          courseTitle: course.title,
          labId: assignment.lab.id,
          labTitle: assignment.lab.title,
          dueAt: assignment.dueAt,
        });
      }
    }
  }
  deadlines.sort((a, b) => Date.parse(a.dueAt) - Date.parse(b.dueAt));
  return deadlines;
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

    await expect(row).toContainText(item.courseTitle);

    const isOverdue = new Date(item.dueAt).getTime() < now;
    await expect(row.getByText(isOverdue ? "OVERDUE" : "DUE", { exact: true })).toBeVisible();
    await expect(row).toContainText(formatDateTime(item.dueAt));

    const dismissIcon = row.locator('[title="Aus Kalender entfernen"]');
    await expect(dismissIcon).toHaveCount(isOverdue ? 1 : 0);
  }

  await expect(page.locator('[title="Aus Kalender entfernen"]')).toHaveCount(overdue.length);
}

const dashboardRoleTestUsers = [
  testUsers.instructor,
  testUsers.admin,
  testUsers.student,
  testUsers.instructorWithoutCoursesOrLabs,
];

test.describe("Dashboard widgets for all primary roles and a user without courses and deadlines", () => {
  // Group: End-to-end dashboard checks (cards, deadlines, hero stats, and empty active labs) per role.
  for (const testUser of dashboardRoleTestUsers) {
    test(`Dashboard data for "${testUser.name}" matches expected test data and ordering rules`, async ({
      page,
    }) => {
      await loginAs(page, testUser);

      const enrolledCoursesCount = testUser.enrolledCourses.length;
      const completedLabsCount = testUser.completedLabs.length;
      const visibleDeadlines = extractDeadlines(testUser.enrolledCourses, testUser.completedLabs);

      await assertCourseCards(page, testUser.enrolledCourses.slice(0, 3));
      await assertUpcomingDeadlines(page, visibleDeadlines);

      await expect(getHeroStatisticValue(page, "Enrolled Courses")).toHaveText(
        String(enrolledCoursesCount)
      );
      await expect(getHeroStatisticValue(page, "Completed Labs")).toHaveText(
        String(completedLabsCount)
      );

      await assertNoActiveLabs(page);
    });
  }
});
