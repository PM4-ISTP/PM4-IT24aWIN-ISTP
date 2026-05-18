import test, { expect, type Page } from "@playwright/test";
import { clickButtonAndAssertUrl, clickNavbarButton } from "@/tests/helpers/navigation";
import { loginAs } from "@/tests/helpers/auth";
import { courses, testUsers, type Course, type ReadonlyLab, type ChallengeCompletion } from "@/tests/data";
import { formatDateTime } from "@/tests/helpers/date";

type LabProgress = {
  solvedLabs: number;
  totalLabs: number;
  solvedChallenges: number;
  totalChallenges: number;
  labPercent: number;
  challengePercent: number;
};

const student = testUsers.student;
const completedLabIds = new Set(student.completedLabs.map((lab) => lab.id));
const completedChallenges = student.completedChallenges;

function calcPercent(done: number, total: number): number {
  if (total === 0) return 0;
  return Math.round((done / total) * 100);
}

function isLabSolved(lab: ReadonlyLab): boolean {
  return completedLabIds.has(lab.id);
}

function getSolvedChallengesCount(lab: ReadonlyLab, completions: readonly ChallengeCompletion[]): number {
  return completions.filter((completion) => completion.lab.id === lab.id).length;
}

function getCourseProgress(course: Course): LabProgress {
  const labs = course.labs.map((assignment) => assignment.lab);
  const solvedLabs = labs.filter((lab) => isLabSolved(lab)).length;
  const totalLabs = labs.length;
  const solvedChallenges = labs.reduce(
    (acc, lab) => acc + getSolvedChallengesCount(lab, completedChallenges),
    0
  );
  const totalChallenges = labs.reduce((acc, lab) => acc + lab.challenges.length, 0);
  return {
    solvedLabs,
    totalLabs,
    solvedChallenges,
    totalChallenges,
    labPercent: calcPercent(solvedLabs, totalLabs),
    challengePercent: calcPercent(solvedChallenges, totalChallenges),
  };
}

function getExpectedDueLabel(dueAt: string, solved: boolean): string {
  const dueDate = new Date(dueAt);
  const deadlinePassed = dueDate.getTime() < Date.now();
  if (deadlinePassed && !solved) {
    return `Expired · ${formatDateTime(dueAt)}`;
  }
  return `Due: ${formatDateTime(dueAt)}`;
}

async function clickCourseCard(page: Page, course: Course, expectedUrl: string) {
  const courseCard = page
    .getByRole("button")
    .filter({ has: page.getByText(course.title ?? "", { exact: true }) })
    .first();
  await expect(courseCard).toBeVisible();
  await clickButtonAndAssertUrl(page, courseCard, expectedUrl);
}

async function openCourseDetailsFromMyCourses(page: Page, course: Course) {
  await clickNavbarButton(page, "MY COURSES", "dashboard/courses");
  await clickCourseCard(page, course, `/dashboard/courses/${course.id}`);
}

async function openCourseDetailsFromCourseCatalog(page: Page, course: Course) {
  await clickNavbarButton(page, "BROWSE / CATALOG", "dashboard/catalog");
  await page.getByRole("textbox", { name: "Search courses" }).fill("E2E");
  await page.getByRole("button", { name: "Search" }).click();
  await clickCourseCard(page, course, `/dashboard/catalog/${course.id}`);
}

async function assertCourseJourneyCard(page: Page, course: Course, isEnrolled: boolean) {
  const progress = getCourseProgress(course);
  const journey = page.getByTestId("course-journey-card");
  await expect(journey).toBeVisible();

  await expect(page.getByTestId("course-instructor-label")).toHaveText(/INSTRUCTOR/i);
  if (course.owner?.name) {
    await expect(page.getByTestId("course-instructor-name")).toHaveText(course.owner.name);
  }
  if (course.owner?.title) {
    await expect(page.getByTestId("course-instructor-title")).toHaveText(course.owner.title);
  }

  if (!isEnrolled) {
    await expect(
      page.getByTestId("unavailable-labs-progress-section").getByText("Not available")
    ).toBeVisible();
  } else {
    await expect(page.getByTestId("course-journey-labs-percent")).toHaveText(
      `${progress.labPercent}% Complete`
    );
    await expect(page.getByTestId("course-journey-labs-solved")).toHaveText(
      `${progress.solvedLabs} Lab${progress.solvedLabs !== 1 ? "s" : ""} Solved`
    );
    await expect(page.getByTestId("course-journey-labs-remaining")).toHaveText(
      `${Math.max(progress.totalLabs - progress.solvedLabs, 0)} Remaining`
    );
  }

  if (!isEnrolled) {
    await expect(
      page.getByTestId("unavailable-challenges-progress-section").getByText("Not available")
    ).toBeVisible();
  } else {
    await expect(page.getByTestId("course-journey-challenges-percent")).toHaveText(
      `${progress.challengePercent}% Complete`
    );
    await expect(page.getByTestId("course-journey-challenges-solved")).toHaveText(
      `${progress.solvedChallenges} Challenge${progress.solvedChallenges !== 1 ? "s" : ""} Solved`
    );
    await expect(page.getByTestId("course-journey-challenges-remaining")).toHaveText(
      `${Math.max(progress.totalChallenges - progress.solvedChallenges, 0)} Remaining`
    );
  }
}

async function assertCourseEnrollmentButton(page: Page, course: Course, isEnrolled: boolean) {
  const hasOpenLabs = course.labs.some((assignment) => !isLabSolved(assignment.lab));
  const button = page.getByTestId("course-enrollment-action");
  if (!isEnrolled) {
    await expect(button).toHaveText("Enroll in Course");
    await expect(button).toBeEnabled();
  } else if (hasOpenLabs) {
    await expect(button).toHaveText("Continue Course");
    await expect(button).toBeEnabled();
  } else {
    await expect(button).toHaveText("All Labs Completed");
    await expect(button).toBeDisabled();
  }
}

async function assertLabList(page: Page, course: Course) {
  if (course.labs.length === 0) {
    await expect(page.locator('[data-testid="course-labs-list"]')).toHaveCount(0);
    return;
  }

  const progress = getCourseProgress(course);
  await expect(page.getByTestId("course-labs-completed")).toHaveText(
    `${progress.solvedLabs}/${progress.totalLabs} completed`
  );

  for (const [index, assignment] of course.labs.entries()) {
    const lab = assignment.lab;
    const solved = isLabSolved(lab);
    const solvedChallenges = getSolvedChallengesCount(lab, completedChallenges);
    const totalChallenges = lab.challenges.length;

    await expect(page.getByTestId(`course-lab-title-${lab.id}`)).toHaveText(
      `#${index + 1} ${lab.title}`
    );
    await expect(page.getByTestId(`course-lab-status-${lab.id}`)).toHaveAttribute(
      "aria-label",
      solved ? "Lab solved" : "Lab not solved"
    );
    await expect(page.getByTestId(`course-lab-challenge-counter-${lab.id}`)).toHaveText(
      `${solvedChallenges}/${totalChallenges}`
    );

    if (assignment.dueAt) {
      await expect(page.getByTestId(`course-lab-due-${lab.id}`)).toHaveText(
        getExpectedDueLabel(assignment.dueAt, solved)
      );
    } else {
      await expect(page.locator(`[data-testid="course-lab-due-${lab.id}"]`)).toHaveCount(0);
    }

    const challengesList = page.getByTestId(`course-lab-challenges-${lab.id}`);
    if (!(await challengesList.isVisible())) {
      await page.getByTestId(`course-lab-toggle-${lab.id}`).click();
    }
    await expect(challengesList).toBeVisible();

    const challengeRows = page.locator(`[data-testid^="course-lab-challenge-${lab.id}-"]`);
    await expect(challengeRows).toHaveCount(lab.challenges.length);

    for (const [challengeIndex, challenge] of lab.challenges.entries()) {
      const row = page.getByTestId(`course-lab-challenge-${lab.id}-${challengeIndex + 1}`);
      await expect(row).toHaveText(new RegExp(`^${challengeIndex + 1}\\.\\s+`));

      const challengeSolved = completedChallenges.some(
        (completion) => completion.challenge.id === challenge.id
      );
      await expect(row.getByLabel(challengeSolved ? "Completed" : "Not completed")).toBeVisible();
    }
  }
}

async function assertCourseDetails(page: Page, course: Course, isEnrolled: boolean) {
  await assertCourseJourneyCard(page, course, isEnrolled);
  await assertLabList(page, course);
  await assertCourseEnrollmentButton(page, course, isEnrolled);
}

const courseCasesMyCourses = [
  courses.instructor04,
  courses.instructor02,
  courses.instructor06,
  courses.admin01,
];

const courseCasesCourseCatalog = [courses.admin01, courses.instructor04, courses.instructor01];

const courseCasesCourseCatalogNotEnrolled = [courses.instructor07, courses.instructor08];

test.describe("Course details in My Courses for e2e-student", () => {
  for (const course of courseCasesMyCourses) {
    test(`Course details for "${course.title}" (My Courses) render expected progress, labs, and challenges`, async ({
      page,
    }) => {
      const isEnrolledInCourse = true;
      await loginAs(page, student);
      await openCourseDetailsFromMyCourses(page, course);
      await assertCourseDetails(page, course, isEnrolledInCourse);
    });
  }
});

test.describe("Course details for enrolled courses in Course Catalog for e2e-student", () => {
  for (const course of courseCasesCourseCatalog) {
    test(`Course details for "${course.title}" (Course Catalog; enrolled) render expected progress, labs, and challenges`, async ({
      page,
    }) => {
      const isEnrolledInCourse = true;
      await loginAs(page, student);
      await openCourseDetailsFromCourseCatalog(page, course);
      await assertCourseDetails(page, course, isEnrolledInCourse);
    });
  }
});

test.describe("Course details for not enrolled courses in Course Catalog for e2e-student", () => {
  for (const course of courseCasesCourseCatalogNotEnrolled) {
    test(`Course details for "${course.title}" (Course Catalog; not enrolled) render expected progress, labs, and challenges`, async ({
      page,
    }) => {
      const isEnrolledInCourse = false;
      await loginAs(page, student);
      await openCourseDetailsFromCourseCatalog(page, course);
      await assertCourseDetails(page, course, isEnrolledInCourse);
    });
  }
});
