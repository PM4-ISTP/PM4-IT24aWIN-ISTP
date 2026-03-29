"use server";

import { getApiClient, fetchBackendMultipartFormData } from "@/src/lib/api/server";

export async function postTest() {
  const client = await getApiClient();
  const { data, error } = await client.POST("/api/v1/tests", {});

  if (error) {
    throw new Error(`Backend error: ${JSON.stringify(error)}`);
  }

  return data;
}

// --- TODO: replace these functions with functions from pull request #54 ---

export async function getAdminConfig() {
  const res = await fetchBackend("/api/admin/config");

  if (!res.ok) {
    throw new Error(`Backend returned ${res.status}: ${res.statusText}`);
  }

  return (await res.json());
}

export async function postAdminConfig(formData: FormData) {
  const res = await fetchBackendMultipartFormData("/api/admin/config", { method: "POST", body: formData });

  if (!res.ok) {
    throw new Error(`Backend returned ${res.status}: ${res.statusText}`);
  }

  return res.body;
}

export async function putAdminConfig(formData: FormData) {
  const res = await fetchBackendMultipartFormData("/api/admin/config", { method: "PUT", body: formData });

  if (!res.ok) {
    throw new Error(`Backend returned ${res.status}: ${res.statusText}`);
  }

  return res.body;
}

export async function deleteAdminConfig() {
  const res = await fetchBackendMultipartFormData("/api/admin/config", { method: "DELETE" });

  if (!res.ok) {
    throw new Error(`Backend returned ${res.status}: ${res.statusText}`);
  }

  return res.body;
}