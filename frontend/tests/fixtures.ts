import { test as base } from "@playwright/test";
import { Client } from "pg";

async function cleanup() {
  const selectCourseIds = "SELECT id FROM courses WHERE title LIKE 'E2E%'";
  const selectChallengeIds = "SELECT id FROM challenges WHERE title LIKE 'E2E%'";
  const selectUserIds = "SELECT id FROM users WHERE username LIKE 'e2e-%'";

  const client = new Client();
  await client.connect();
  try {
    await client.query("BEGIN");
    await client.query(`DELETE FROM user_course_badges WHERE user_id IN (${selectUserIds})`);
    await client.query(`DELETE FROM challenge_completions WHERE user_id IN (${selectUserIds})`);
    await client.query(
      `DELETE FROM student_option_submissions WHERE user_id IN (${selectUserIds})`
    );
    await client.query(`DELETE FROM student_flag_submissions WHERE user_id IN (${selectUserIds})`);
    await client.query(`DELETE FROM course_enrollments WHERE participant_id IN (${selectUserIds})`);
    await client.query(`DELETE FROM course_labs WHERE course_id IN (${selectCourseIds})`);
    await client.query(`DELETE FROM course_instructors WHERE instructor_id IN (${selectUserIds})`);
    await client.query("DELETE FROM courses WHERE title LIKE 'E2E%'");
    await client.query(
      `DELETE FROM challenge_options WHERE sub_task_id IN (${selectChallengeIds})`
    );
    await client.query("DELETE FROM challenges WHERE title LIKE 'E2E%'");
    await client.query("DELETE FROM labs WHERE title LIKE 'E2E%'");
    await client.query("DELETE FROM course_topics WHERE topic LIKE 'E2E%'");
    await client.query("COMMIT");
  } catch (e) {
    await client.query("ROLLBACK");
    throw e;
  } finally {
    await client.end();
  }
}

export const test = base.extend<{ forEachTest: void }>({
  forEachTest: [
    async ({}, use) => {
      await use();
      await cleanup();
    },
    { auto: true },
  ],
});
