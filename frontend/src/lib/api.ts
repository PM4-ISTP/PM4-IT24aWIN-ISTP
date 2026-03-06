import { getServerSession } from "next-auth";
import { getToken } from "next-auth/jwt";
import { authOptions } from "@/src/lib/auth";
import { cookies, headers } from "next/headers";

const BACKEND_URL = process.env.BACKEND_URL || "http://localhost:8080";

/**
 * Server-side fetch wrapper that automatically attaches the Keycloak
 * access token as a Bearer token for Spring Boot API calls.
 *
 * The access token is read from the encrypted JWT cookie (server-side only)
 * and is never exposed to the client browser.
 */
export async function fetchBackend(
  path: string,
  options: RequestInit = {},
): Promise<Response> {
  const token = await getToken({
    req: {
      headers: Object.fromEntries((await headers()).entries()),
      cookies: Object.fromEntries(
        (await cookies()).getAll().map((c) => [c.name, c.value]),
      ),
    } as any,
    secret: process.env.NEXTAUTH_SECRET,
  });

  if (!token?.accessToken) {
    throw new Error("Not authenticated");
  }

  const url = `${BACKEND_URL}${path}`;
  const reqHeaders = new Headers(options.headers);
  reqHeaders.set("Authorization", `Bearer ${token.accessToken}`);
  reqHeaders.set("Content-Type", "application/json");

  return fetch(url, {
    ...options,
    headers: reqHeaders,
  });
}
