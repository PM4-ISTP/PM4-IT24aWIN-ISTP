"use server";

import { getApiClient } from "@/src/lib/api/server";

export async function postTest() {
  const client = await getApiClient();
  const { data, error } = await client.POST("/api/v1/tests", {});

  if (error) {
    throw new Error(`Backend error: ${JSON.stringify(error)}`);
  }

  return data;
}
