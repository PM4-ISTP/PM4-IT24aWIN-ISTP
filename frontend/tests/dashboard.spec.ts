import test, { expect, type Page } from "@playwright/test";
import { loginAs, TestUser } from "@/tests/helpers";

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

type EnrollmentsResponse = {
  content?: DashboardCourse[];
  totalElements?: number;
};

type DeadlineDto = {
  courseId?: string;
  courseTitle?: string;
  labId?: string;
  labTitle?: string;
  dueAt?: string;
};

type NormalizedDeadline = {
  courseId: string;
  courseTitle: string;
  labId: string;
  labTitle: string;
  dueAt: string;
};

function normalizeWhitespace(value: string): string {
  return value.replace(/\s+/g, " ").trim();
}

function escapeRegExp(value: string): string {
  return value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

function getCourseStatusLabel(course: DashboardCourse): "Private" | "Published" | "Draft" {
  if (course.isPrivate) return "Private";
  if (course.isPublished) return "Published";
  return "Draft";
}

function getCoursePreviewText(course: DashboardCourse): string {
  const normalizedShortDescription = normalizeWhitespace(course.shortDescription ?? "");
  if (normalizedShortDescription) {
    return normalizedShortDescription;
  }

  return normalizeWhitespace(
    (course.description ?? "").replace(/<\/(p|h[1-6]|li|br|div)>/gi, " ").replace(/<[^>]*>/g, " ")
  );
}

function formatCourseDate(updatedAt?: string | null): string {
  if (!updatedAt) {
    return "No date specified";
  }

  return new Date(updatedAt).toLocaleDateString("de-CH", {
    day: "numeric",
    month: "short",
    year: "numeric",
  });
}

function formatDeadlineDate(dueAt: string): string {
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

async function getJson<T>(page: Page, path: string): Promise<T> {
  const response = await page.request.get(path);
  expect(response.ok(), `GET ${path} should succeed`).toBeTruthy();
  return (await response.json()) as T;
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
      await expect(
        card.getByText(course.ownerTitle ?? "Instructor", { exact: true })
      ).toBeVisible();
    }

    const preview = getCoursePreviewText(course);
    if (preview) {
      await expect(card).toContainText(preview.slice(0, 40));
    }

    await expect(card).toContainText(formatCourseDate(course.updatedAt));
  }
}

function normalizeDeadlines(deadlines: DeadlineDto[]): NormalizedDeadline[] {
  return deadlines
    .filter((item) => item.courseId && item.labId && item.dueAt)
    .map((item) => ({
      courseId: String(item.courseId),
      courseTitle: String(item.courseTitle ?? ""),
      labId: String(item.labId),
      labTitle: String(item.labTitle ?? ""),
      dueAt: String(item.dueAt),
    }))
    .filter((item) => !Number.isNaN(new Date(item.dueAt).getTime()))
    .slice(0, 8);
}

async function assertUpcomingDeadlines(page: Page, rawDeadlines: DeadlineDto[]) {
  const now = Date.now();
  const visibleDeadlines = normalizeDeadlines(rawDeadlines);

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
    await expect(row).toContainText(item.courseTitle || "-");

    const isOverdue = new Date(item.dueAt).getTime() < now;
    await expect(row.getByText(isOverdue ? "OVERDUE" : "DUE", { exact: true })).toBeVisible();
    await expect(row).toContainText(formatDeadlineDate(item.dueAt));

    const dismissIcon = row.locator('[title="Aus Kalender entfernen"]');
    await expect(dismissIcon).toHaveCount(isOverdue ? 1 : 0);
  }

  await expect(page.locator('[title="Aus Kalender entfernen"]')).toHaveCount(overdue.length);
}

const DASHBOARD_ROLE_CASES: Array<{ roleName: string; user: TestUser }> = [
  { roleName: "Instructor", user: TestUser.Instructor },
  { roleName: "Admin", user: TestUser.Admin },
  { roleName: "Student", user: TestUser.Student },
];

test.describe("Dashboard widgets for all primary roles", () => {
  // Group: End-to-end dashboard checks (cards, deadlines, hero stats, and empty active labs) per role.
  for (const { roleName, user } of DASHBOARD_ROLE_CASES) {
    test(`Dashboard data for ${roleName} matches backend sources and ordering rules`, async ({
      page,
    }) => {
      await loginAs(page, user);

      const [enrollments, deadlines, completedLabs, runningPods] = await Promise.all([
        getJson<EnrollmentsResponse>(
          page,
          "/api/backend/api/v1/courses/my-enrollments?page=0&size=3"
        ),
        getJson<DeadlineDto[]>(page, "/api/backend/api/v1/courses/my-deadlines"),
        getJson<{ count?: number }>(page, "/api/backend/api/v1/labs/my-completed-count"),
        getJson<Array<{ labId?: string }>>(page, "/api/backend/api/v1/lab-pods"),
      ]);

      const visibleCourses = enrollments.content ?? [];
      await assertContinueLearning(page, visibleCourses);
      await assertUpcomingDeadlines(page, deadlines);

      await expect(getHeroStatisticValue(page, "Enrolled Courses")).toHaveText(
        String(enrollments.totalElements ?? 0)
      );
      await expect(getHeroStatisticValue(page, "Completed Labs")).toHaveText(
        String(completedLabs.count ?? 0)
      );

      expect(runningPods.length, "Expected no running lab pods for dashboard baseline").toBe(0);
      await expect(page.getByText("No active labs", { exact: true })).toBeVisible();
    });
  }
});
