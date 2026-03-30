import { getToken } from "next-auth/jwt";
import { cookies, headers } from "next/headers";
import type { GetTokenParams } from "next-auth/jwt";
import { createApiClient } from ".";

const BACKEND_URL = process.env.BACKEND_URL ?? "http://localhost:8080";

export async function getApiClient() {
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

  return createApiClient(BACKEND_URL, token.accessToken);
}
