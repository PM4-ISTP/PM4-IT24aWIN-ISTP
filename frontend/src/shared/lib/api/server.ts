import { getServerSession } from "next-auth";
import type { JWT } from "next-auth/jwt";
import { encode, getToken } from "next-auth/jwt";
import { cookies, headers } from "next/headers";
import { createApiClient } from ".";
import { authOptions, REFRESH_BUFFER_MS, refreshAccessToken } from "../auth";
import type { GetTokenParams } from "next-auth/jwt";

const BACKEND_URL = process.env.BACKEND_URL ?? "http://localhost:8080";

/**
 * Server-side fetch that opts out of Next.js's request-deduplication / data
 * cache. Dashboard data is per-user and request-scoped, so caching could
 * serve stale or — worse — incorrectly shared data between users.
 */
const noStoreFetch: typeof fetch = (input, init) => fetch(input, { cache: "no-store", ...init });

const COOKIE_CHUNK_SIZE = 4096 - 163;
const SESSION_MAX_AGE_SECONDS = 30 * 24 * 60 * 60;

type SessionCookieUpdate = {
  name: string;
  value: string;
  options: {
    httpOnly: true;
    sameSite: "lax";
    path: "/";
    secure: boolean;
    expires?: Date;
    maxAge?: number;
  };
};

async function buildReqFromNextHeaders(): Promise<GetTokenParams["req"]> {
  return {
    headers: Object.fromEntries((await headers()).entries()),
    cookies: Object.fromEntries((await cookies()).getAll().map((c) => [c.name, c.value])),
  } as unknown as GetTokenParams["req"];
}

type GetValidAccessTokenOptions = {
  forceRefresh?: boolean;
};

function shouldUseSecureCookies(): boolean {
  return process.env.NEXTAUTH_URL?.startsWith("https://") ?? false;
}

function sessionCookieName(): string {
  return `${shouldUseSecureCookies() ? "__Secure-" : ""}next-auth.session-token`;
}

function getExistingSessionCookieNames(req: GetTokenParams["req"], baseName: string): string[] {
  const requestCookies = req.cookies;

  if (typeof requestCookies?.getAll === "function") {
    return requestCookies
      .getAll()
      .map((cookie) => cookie.name)
      .filter((name) => name === baseName || name.startsWith(`${baseName}.`));
  }

  return Object.keys(requestCookies ?? {}).filter(
    (name) => name === baseName || name.startsWith(`${baseName}.`)
  );
}

async function createSessionCookies(
  req: GetTokenParams["req"],
  token: JWT
): Promise<SessionCookieUpdate[]> {
  const secret = process.env.NEXTAUTH_SECRET;
  if (!secret) {
    throw new Error("Missing NEXTAUTH_SECRET");
  }

  const maxAge = authOptions.session?.maxAge ?? SESSION_MAX_AGE_SECONDS;
  const baseName = sessionCookieName();
  const encodedToken = await encode({ token, secret, maxAge });
  const expires = new Date(Date.now() + maxAge * 1000);
  const secure = shouldUseSecureCookies();
  const baseOptions = {
    httpOnly: true,
    sameSite: "lax",
    path: "/",
    secure,
  } as const;

  // Match NextAuth's session-cookie chunking so large encrypted JWTs keep working.
  const newCookies: SessionCookieUpdate[] =
    encodedToken.length <= COOKIE_CHUNK_SIZE
      ? [
          {
            name: baseName,
            value: encodedToken,
            options: { ...baseOptions, expires },
          },
        ]
      : Array.from({ length: Math.ceil(encodedToken.length / COOKIE_CHUNK_SIZE) }, (_, index) => ({
          name: `${baseName}.${index}`,
          value: encodedToken.slice(index * COOKIE_CHUNK_SIZE, (index + 1) * COOKIE_CHUNK_SIZE),
          options: { ...baseOptions, expires },
        }));

  const newNames = new Set(newCookies.map((cookie) => cookie.name));
  const staleCookies = getExistingSessionCookieNames(req, baseName)
    .filter((name) => !newNames.has(name))
    .map<SessionCookieUpdate>((name) => ({
      name,
      value: "",
      options: { ...baseOptions, maxAge: 0 },
    }));

  return [...staleCookies, ...newCookies];
}

async function persistSessionCookies(sessionCookies: SessionCookieUpdate[]): Promise<void> {
  const cookieStore = await cookies();

  for (const cookie of sessionCookies) {
    cookieStore.set(cookie.name, cookie.value, cookie.options);
  }
}

export async function getValidAccessToken(
  req?: GetTokenParams["req"],
  options: GetValidAccessTokenOptions = {}
): Promise<string> {
  const authReq = req ?? (await buildReqFromNextHeaders());
  const token = await getToken({
    req: authReq,
    secret: process.env.NEXTAUTH_SECRET,
  });

  if (!token || typeof token.accessToken !== "string" || token.accessToken.length === 0) {
    throw new Error("Not authenticated");
  }

  const expiresAtMs = typeof token.accessTokenExpires === "number" ? token.accessTokenExpires : 0;
  if (!options.forceRefresh && Date.now() < expiresAtMs - REFRESH_BUFFER_MS) {
    return token.accessToken;
  }

  const refreshedToken = await refreshAccessToken(token);
  if (
    refreshedToken.error ||
    typeof refreshedToken.accessToken !== "string" ||
    refreshedToken.accessToken.length === 0
  ) {
    throw new Error("Not authenticated");
  }

  const sessionCookies = await createSessionCookies(authReq, refreshedToken);
  await persistSessionCookies(sessionCookies);
  return refreshedToken.accessToken;
}

export async function getApiClient() {
  // Keep getServerSession() to preserve existing NextAuth behavior (callbacks, session state).
  await getServerSession(authOptions);
  const accessToken = await getValidAccessToken();
  return createApiClient(BACKEND_URL, accessToken, noStoreFetch);
}
