"use server";

import { fetchBackend } from "@/src/lib/api";

export async function postTest() {
  const res = await fetchBackend("/api/v1/tests", { method: "POST" });

  if (!res.ok) {
    throw new Error(`Backend returned ${res.status}: ${res.statusText}`);
  }

  return res.json();
}
