import { expect, type Page } from "@playwright/test";
import { type Course } from "@/tests/data";

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

export async function assertCourseCards(page: Page, courses: readonly Course[]) {
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

    if (course.owner?.name) {
      await expect(card.getByText(course.owner.name, { exact: true })).toBeVisible();
      await expect(card.getByText(course.owner.title ?? "", { exact: true })).toBeVisible();
    }

    await expect(card).toContainText(course.shortDescription ?? "");
    await expect(card).toContainText(formatDate(course.updatedAt));
  }
}
