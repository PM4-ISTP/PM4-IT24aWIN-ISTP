"use server";

import { fetchBackend, fetchBackendMultipartFormData } from "@/src/lib/api";

export async function postTest() {
  const res = await fetchBackend("/api/v1/tests", { method: "POST" });

  if (!res.ok) {
    throw new Error(`Backend returned ${res.status}: ${res.statusText}`);
  }

  return (await res.json()) as unknown;
}

// TODO: replace this function with functions from pull request #54
export async function postAdminConfig(formData: FormData) {
  const res = await fetchBackendMultipartFormData("/api/admin/config", { method: "POST", body: formData });

  if (!res.ok) {
    throw new Error(`Backend returned ${res.status}: ${res.statusText}`);
  }

  return res.body;
}