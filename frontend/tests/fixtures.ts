import { test as base } from "@playwright/test";
import { Client } from "pg";

async function cleanup() {
  const client = new Client();
  await client.connect();
  try {
    await client.query("BEGIN");
    await client.query("DELETE FROM challenges WHERE title LIKE 'E2E%'");
    await client.query("DELETE FROM labs WHERE title LIKE 'E2E%'");
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
