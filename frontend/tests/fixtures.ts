import { test as base } from "@playwright/test";
import { Client } from "pg";
import fs from "node:fs";

async function setup() {
  // file path is relative to the folder "frontend"
  const insertTestdata = fs.readFileSync("tests/files/setup.sql", "utf8");
  const client = new Client();
  await client.connect();
  try {
    await client.query("BEGIN");
    await client.query(insertTestdata);
    await client.query("COMMIT");
  } catch (e) {
    await client.query("ROLLBACK");
    throw e;
  } finally {
    await client.end();
  }
}

async function cleanup() {
  // file path is relative to the folder "frontend"
  const deleteTestdata = fs.readFileSync("tests/files/cleanup.sql", "utf8");
  const client = new Client();
  await client.connect();
  try {
    await client.query("BEGIN");
    await client.query(deleteTestdata);
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
      await setup();
      await use();
      await cleanup();
    },
    { auto: true },
  ],
});
