import test, { expect, type Page } from "@playwright/test";
import { loginAs, TestUser } from "@/tests/helpers";
import testData from "@/tests/files/dashboard-test-data.json";
import adminCourse01 from "@/tests/files/courses/admin_01.json";
import instructorCourse01 from "@/tests/files/courses/instructor_01.json";
import instructorCourse02 from "@/tests/files/courses/instructor_02.json";
import instructorCourse04 from "@/tests/files/courses/instructor_04.json";

type DashboardCourse = {
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
  dataKey: keyof typeof testData.dashboardTestData;
  displayedCourses: DashboardCourse[];
};

function escapeRegExp(value: string): string {
  return value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

function getCourseStatusLabel(course: DashboardCourse): "Private" | "Published" | "Draft" {
  if (course.isPrivate) return "Private";
  if (course.isPublished) return "Published";
  return "Draft";
}

function formatCourseDate(updatedAt?: string | null | number): string {
  if (!updatedAt) {
    return "No date specified";
  }

  return new Date(typeof updatedAt === "number" ? updatedAt : updatedAt).toLocaleDateString(
    "de-CH",
    {
      day: "numeric",
      month: "short",
      year: "numeric",
    }
  );
}

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

async function assertContinueLearning(page: Page, courses: DashboardCourse[]) {
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
    await expect(card).toContainText(formatCourseDate(course.updatedAt));
  }
}

async function assertUpcomingDeadlines(page: Page, visibleDeadlines: Deadline[]) {
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
];

test.describe("Dashboard widgets for all primary roles", () => {
  // Group: End-to-end dashboard checks (cards, deadlines, hero stats, and empty active labs) per role.
  for (const { roleName, user, dataKey, displayedCourses } of DASHBOARD_ROLE_CASES) {
    test(`Dashboard data for ${roleName} matches expected test data and ordering rules`, async ({
      page,
    }) => {
      await loginAs(page, user);

      const expectedData = testData.dashboardTestData[dataKey];
      const deadlines = expectedData.upcomingDeadlines;

      await assertContinueLearning(page, displayedCourses);
      await assertUpcomingDeadlines(page, deadlines);

      await expect(getHeroStatisticValue(page, "Enrolled Courses")).toHaveText(
        String(expectedData.enrolledCoursesCount)
      );
      await expect(getHeroStatisticValue(page, "Completed Labs")).toHaveText(
        String(expectedData.completedLabsCount)
      );

      // Verify no active labs
      const runningPods = await page.request.get("/api/backend/api/v1/lab-pods");
      const podsData = (await runningPods.json()) as Array<{ labId?: string }>;
      expect(podsData.length, "Expected no running lab pods for dashboard baseline").toBe(0);
      await expect(page.getByText("No active labs", { exact: true })).toBeVisible();
    });
  }
});
