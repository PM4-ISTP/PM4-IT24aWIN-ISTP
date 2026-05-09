import { getServerSession } from "next-auth";
import { getToken } from "next-auth/jwt";
import { cookies, headers } from "next/headers";
import { createApiClient } from ".";
import { authOptions } from "../auth";
import type { GetTokenParams } from "next-auth/jwt";

const BACKEND_URL = process.env.BACKEND_URL ?? "http://localhost:8080";

export async function getApiClient() {
  // Trigger NextAuth callbacks (incl. refresh logic) before reading the JWT.
  // The access token itself is intentionally not exposed on the Session object.
  await getServerSession(authOptions);

  const req_ = {
    headers: Object.fromEntries((await headers()).entries()),
    cookies: Object.fromEntries((await cookies()).getAll().map((c) => [c.name, c.value])),
  };

  const token = await getToken({
    req: req_ as GetTokenParams["req"],
    secret: process.env.NEXTAUTH_SECRET,
  });

  if (typeof token?.accessToken !== "string" || token.accessToken.length === 0) {
    throw new Error("Not authenticated");
  }

  return createApiClient(BACKEND_URL, token.accessToken);
}
