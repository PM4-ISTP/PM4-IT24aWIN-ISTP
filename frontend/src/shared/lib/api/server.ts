import { getServerSession } from "next-auth";
import { getToken } from "next-auth/jwt";
import { cookies, headers } from "next/headers";
import { createApiClient } from ".";
import { authOptions } from "../auth";
import type { GetTokenParams } from "next-auth/jwt";

const BACKEND_URL = process.env.BACKEND_URL ?? "http://localhost:8080";

const REFRESH_BUFFER_MS = 60_000;
const refreshInProgress = new Map<string, Promise<string>>();

async function buildReqFromNextHeaders(): Promise<GetTokenParams["req"]> {
  return {
    headers: Object.fromEntries((await headers()).entries()),
    cookies: Object.fromEntries((await cookies()).getAll().map((c) => [c.name, c.value])),
  } as unknown as GetTokenParams["req"];
}

async function doRefreshAccessToken(refreshToken: string): Promise<string> {
  const issuer = process.env.AUTH_KEYCLOAK_ISSUER;
  const clientId = process.env.AUTH_KEYCLOAK_ID;
  const clientSecret = process.env.AUTH_KEYCLOAK_SECRET;

  if (!issuer || !clientId || !clientSecret) {
    throw new Error("Missing Keycloak configuration");
  }

  const response = await fetch(`${issuer}/protocol/openid-connect/token`, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      client_id: clientId,
      client_secret: clientSecret,
      grant_type: "refresh_token",
      refresh_token: refreshToken,
    }),
    cache: "no-store",
  });

  if (!response.ok) {
    throw new Error("Failed to refresh access token");
  }

  const refreshedTokens = (await response.json()) as {
    access_token?: string;
    expires_in?: number;
    refresh_token?: string;
  };

  if (typeof refreshedTokens.access_token !== "string" || refreshedTokens.access_token.length === 0) {
    throw new Error("Failed to refresh access token");
  }

  return refreshedTokens.access_token;
}

export async function getValidAccessToken(req?: GetTokenParams["req"]): Promise<string> {
  const token = await getToken({
    req: req ?? (await buildReqFromNextHeaders()),
    secret: process.env.NEXTAUTH_SECRET,
  });

  if (!token || typeof token.accessToken !== "string" || token.accessToken.length === 0) {
    throw new Error("Not authenticated");
  }

  const expiresAtMs = typeof token.accessTokenExpires === "number" ? token.accessTokenExpires : 0;
  if (Date.now() < expiresAtMs - REFRESH_BUFFER_MS) {
    return token.accessToken;
  }

  const refreshToken = typeof token.refreshToken === "string" ? token.refreshToken : "";
  if (refreshToken.length === 0) {
    throw new Error("Not authenticated");
  }

  const existing = refreshInProgress.get(refreshToken);
  if (existing) {
    return existing;
  }

  const refreshPromise = doRefreshAccessToken(refreshToken).finally(() => {
    refreshInProgress.delete(refreshToken);
  });

  refreshInProgress.set(refreshToken, refreshPromise);
  return refreshPromise;
}

export async function getApiClient() {
  // Keep getServerSession() to preserve existing NextAuth behavior (callbacks, session state).
  await getServerSession(authOptions);
  const accessToken = await getValidAccessToken();
  return createApiClient(BACKEND_URL, accessToken);
}
