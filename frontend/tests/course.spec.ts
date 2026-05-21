import { test } from "@/tests/fixtures";
import { loginAs } from "@/tests/helpers/auth";
import { courses, labs, testUsers, Topic } from "@/tests/data";
import {
  clickButtonAndAssert,
  clickButtonAndAssertUrl,
  clickNavbarButton,
} from "@/tests/helpers/navigation";
import { expect } from "@playwright/test";

const courseTabName = "DASHBOARD";
const courseTabUrl = "dashboard/instructor";
const enrollmentButtonTestId = "course-enrollment-action";

test("Instructor can create a course and edit it afterwards (e.g. change title, add lab).", async ({
  page,
}) => {
  test.setTimeout(120_000);
  const newCourseData = {
    title: "E2E Test Course: Create Course Test",
    shortDescription: "Course for E2E test (create course)",
    description: "This course is for E2E testing (course creation).",
    topic: Topic.SecondTopic,
  };
  const updatedTitle = "E2E Test Course: Update Newly Created Course Test";
  const lab = labs.instructor01;
  const owner = testUsers.instructor;
  const collaborator = testUsers.instructorWithoutCoursesOrLabs;

  await loginAs(page, owner);
  await clickNavbarButton(page, courseTabName, courseTabUrl);

  // Create course
  await page.getByRole("link", { name: "New course" }).click();
  await page.getByRole("textbox", { name: "Course Title" }).click();
  await page.getByRole("textbox", { name: "Course Title" }).fill(newCourseData.title);
  await page.getByRole("textbox", { name: "Short Description" }).click();
  await page
    .getByRole("textbox", { name: "Short Description" })
    .fill(newCourseData.shortDescription);
  await page.getByRole("textbox").filter({ hasText: "Add a description..." }).click();
  await page
    .getByRole("textbox")
    .filter({ hasText: "Add a description..." })
    .press("ControlOrMeta+a");
  await page
    .getByRole("textbox")
    .filter({ hasText: "Add a description..." })
    .fill(newCourseData.description);
  await page.getByRole("combobox", { name: "Topic" }).click();
  await page.getByRole("listbox").getByText(newCourseData.topic).click();
  await page.getByRole("combobox", { name: "Collaborators" }).click();
  await page
    .getByRole("listbox")
    .getByText(testUsers.instructorWithoutCoursesOrLabs.username)
    .click();
  await page.locator("label").filter({ hasText: "Public" }).click();
  await page.locator("label").filter({ hasText: "Once" }).click();
  await page.getByRole("button", { name: "Create Course" }).click();
  await clickButtonAndAssert(
    () => page.getByRole("button", { name: "Create Course" }),
    async () =>
      expect(page.getByRole("heading", { name: "Edit Course" })).toBeVisible({ timeout: 20_000 })
  );

  // Verify course created
  await expect(page.getByLabel("Course Title")).toHaveValue(newCourseData.title);
  await expect(page.getByLabel("Short Description")).toHaveValue(newCourseData.shortDescription);
  await expect(page.getByLabel("Topic").first()).toHaveValue(newCourseData.topic);
  await expect(page.getByText(newCourseData.description, { exact: true })).toBeVisible();

  // Verfify owner, collaborator and participants set (owner is automatically enrolled, collaborators are not)
  const peoplePanel = page.getByTestId("course-people-panel");
  await expect(peoplePanel.getByText(owner.name, { exact: true }).first()).toBeVisible();
  await expect(peoplePanel.getByText(collaborator.name, { exact: true })).toBeVisible();
  await expect(peoplePanel.getByText(owner.name, { exact: true }).nth(1)).toBeVisible();

  // Edit course after creation (change title and add one lab)
  await page.getByRole("textbox", { name: "Course Title" }).click();
  await page.getByRole("textbox", { name: "Course Title" }).press("ControlOrMeta+a");
  await page.getByRole("textbox", { name: "Course Title" }).fill(updatedTitle);
  await page.getByRole("textbox", { name: "Search labs to add..." }).click();
  await page.getByRole("textbox", { name: "Search labs..." }).click();
  await page.getByRole("textbox", { name: "Search labs..." }).fill("E2E");
  await page.getByRole("button", { name: lab.title }).click();
  const closeModalButton = page
    .locator("section")
    .filter({ hasText: "Add Lab to Course" })
    .getByRole("button")
    .first();
  await closeModalButton.click();
  await page.getByRole("textbox", { name: "Due date & time" }).click();
  await page.getByRole("textbox", { name: "Due date & time" }).press("Tab");
  await page.getByRole("textbox", { name: "Due date & time" }).fill("2100-01-01T11:00");
  await page.getByRole("button", { name: "Save Changes" }).click();
  await clickButtonAndAssertUrl(
    page,
    () => page.getByRole("button", { name: "Back to dashboard" }),
    courseTabUrl
  );

  // Verify course edit
  const courseCard = page.getByRole("button", { name: updatedTitle });
  await expect(courseCard).toBeVisible();
});

test("Instructor can delete a course using the edit view.", async ({ page }) => {
  const courseUnderTest = courses.instructor01;
  await loginAs(page, testUsers.instructor);
  await clickNavbarButton(page, courseTabName, courseTabUrl);

  // Delete course
  await clickButtonAndAssertUrl(
    page,
    () => page.getByRole("button", { name: courseUnderTest.title }),
    `${courseTabUrl}/${courseUnderTest.id}`
  );
  await page.getByRole("button", { name: "Delete Course" }).click();
  await clickButtonAndAssertUrl(
    page,
    () => page.getByLabel("Delete Course").getByRole("button", { name: "Delete Course" }),
    courseTabUrl
  );

  // Verify course deleted
  const courseCard = page.getByRole("button", { name: courseUnderTest.title });
  await expect(courseCard).not.toBeVisible();
});

test("Student can join course via catalog.", async ({ page }) => {
  const courseUnderTest = courses.instructor09;
  await loginAs(page, testUsers.student);
  await clickNavbarButton(page, "Browse / Catalog", "dashboard/catalog");
  await page.getByRole("textbox", { name: "Search courses" }).fill(courseUnderTest.title);
  await page.getByRole("button", { name: "Search" }).click();
  await clickButtonAndAssertUrl(
    page,
    () => page.getByRole("button", { name: courseUnderTest.title }),
    `dashboard/catalog/${courseUnderTest.id}`
  );
  await expect(page.getByTestId(enrollmentButtonTestId)).toHaveText("Enroll in Course");
  await page.getByTestId(enrollmentButtonTestId).click();
  await expect(page.getByTestId(enrollmentButtonTestId)).toHaveText("Continue Course");
});

test("Student can join course via invite code.", async ({ page }) => {
  const courseUnderTest = courses.instructor05;
  const inviteCode = courseUnderTest.inviteCode ?? "";

  await loginAs(page, testUsers.student);
  await page.getByRole("button", { name: "add Join course" }).click();
  for (let i = 0; i < inviteCode.length; i++) {
    await page.getByRole("textbox", { name: "PinInput" }).nth(i).click();
    await page
      .getByRole("textbox", { name: "PinInput" })
      .nth(i)
      .fill(inviteCode.at(i) ?? "");
  }
  await clickButtonAndAssertUrl(
    page,
    () => page.getByRole("button", { name: "Join Course", exact: true }),
    `dashboard/catalog/${courseUnderTest.id}`
  );
  await expect(page.getByRole("heading", { name: "E2E Test Course: Instructor" })).toBeVisible();
});

test("Student can leave course.", async ({ page }) => {
  const courseUnderTest = courses.instructor01;
  await loginAs(page, testUsers.student);
  await clickNavbarButton(page, "My Courses", "dashboard/courses");
  await clickButtonAndAssertUrl(
    page,
    () => page.getByRole("button", { name: courseUnderTest.title }),
    `dashboard/courses/${courseUnderTest.id}`
  );
  await page.getByRole("button", { name: "Leave Course" }).click();
  await page.getByLabel("Leave Course").getByRole("button", { name: "Leave Course" }).click();
  await expect(page.getByTestId(enrollmentButtonTestId)).toHaveText("Enroll in Course");
});
