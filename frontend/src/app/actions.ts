"use server";

import { fetchBackend } from "@/src/lib/api";
import type { GetTokenParams } from "next-auth/jwt";
import { getToken } from "next-auth/jwt";
import { cookies, headers } from "next/headers";

export async function postTest() {
  const res = await fetchBackend("/api/v1/tests", { method: "POST" });

  if (!res.ok) {
    throw new Error(`Backend returned ${res.status}: ${res.statusText}`);
  }

  return (await res.json()) as unknown;
}

export async function fetchCoursesTests() {
  const res = await fetchBackend("/api/v1/courses", { method: "GET" });

  if (!res.ok) {
    throw new Error(`Backend returned ${res.status}: ${res.statusText}`);
  }

  return (await res.json()) as unknown;
}

export async function postCourse() {
  const req = {
    headers: Object.fromEntries((await headers()).entries()),
    cookies: Object.fromEntries((await cookies()).getAll().map((c) => [c.name, c.value])),
  };

  const token = await getToken({
    req: req as GetTokenParams["req"],
    secret: process.env.NEXTAUTH_SECRET,
  });

  if (!token?.accessToken) {
    throw new Error("Not authenticated");
  }

  const instructorId = token.sub; // Keycloak User UUID

  const res = await fetchBackend("/api/v1/courses", {
    method: "POST",
    body: JSON.stringify({
      title: "Introduction to TypeScript",
      description: "A beginner-friendly course covering TypeScript fundamentals.",
      isPublished: false,
      instructor: {
        instructorRole: "OWNER",
        instructorId,
      },
    }),
  });

  if (!res.ok) {
    throw new Error(`Backend returned ${res.status}: ${res.statusText}`);
  }

  return (await res.json()) as unknown;
}
