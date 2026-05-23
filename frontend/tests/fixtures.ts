import { test as base } from "@playwright/test";

const backendUrl = process.env.BACKEND_URL ?? "http://localhost:8080";
const databaseCredentials = {
  username: process.env.DATABASE_USERNAME ?? "postgres",
  password: process.env.DATABASE_PASSWORD ?? "postgres",
};

async function callTestingEndpoint(path: string) {
  const url = new URL(path, backendUrl);
  let response: Response;
  try {
    response = await fetch(url.toString(), {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(databaseCredentials),
    });
  } catch (error) {
    throw new Error(`Testing endpoint is unreachable: ${url.toString()}`, { cause: error });
  }
  if (!response.ok) {
    const body = await response.text();
    throw new Error(`Testing endpoint failed (${response.status} ${response.statusText}): ${body}`);
  }
}

async function setup() {
  await callTestingEndpoint("/api/v1/testing/cleanup-testdata");
  await callTestingEndpoint("/api/v1/testing/load-testdata");
}

async function cleanup() {
  await callTestingEndpoint("/api/v1/testing/cleanup-testdata");
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
