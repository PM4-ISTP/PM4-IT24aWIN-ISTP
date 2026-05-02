import type { AuthOptions } from "next-auth";
import type { JWT } from "next-auth/jwt";
import KeycloakProvider from "next-auth/providers/keycloak";
import type { KeycloakJwt } from "../types/next-auth";
import { jwtDecode } from "jwt-decode";

type KeycloakProfileClaims = {
  roles: string[];
  name?: string;
  email?: string;
  picture?: string;
};

type KeycloakBasicProfile = Pick<KeycloakProfileClaims, "name" | "email" | "picture">;

function extractBasicProfile(
  claims: Pick<KeycloakJwt, "name" | "preferred_username" | "email" | "picture">
): KeycloakBasicProfile {
  return {
    name:
      typeof claims.name === "string"
        ? claims.name
        : typeof claims.preferred_username === "string"
          ? claims.preferred_username
          : undefined,
    email: typeof claims.email === "string" ? claims.email : undefined,
    picture: typeof claims.picture === "string" ? claims.picture : undefined,
  };
}

function extractProfileClaims(accessToken: string): KeycloakProfileClaims {
  const decoded = jwtDecode<KeycloakJwt>(accessToken);

  return {
    roles: decoded.realm_access?.roles ?? [],
    ...extractBasicProfile(decoded),
  };
}

function mergeProfileClaims(token: JWT, claims: KeycloakProfileClaims): JWT {
  return {
    ...token,
    roles: claims.roles,
    name: claims.name ?? token.name,
    email: claims.email ?? token.email,
    picture: claims.picture ?? token.picture,
  };
}

async function fetchUserInfo(accessToken: string): Promise<KeycloakBasicProfile> {
  if (!process.env.AUTH_KEYCLOAK_ISSUER) {
    return {};
  }

  try {
    const response = await fetch(
      `${process.env.AUTH_KEYCLOAK_ISSUER}/protocol/openid-connect/userinfo`,
      {
        headers: {
          Authorization: `Bearer ${accessToken}`,
        },
        cache: "no-store",
      }
    );

    if (!response.ok) {
      throw new Error("Failed to fetch user info");
    }

    const userInfo = (await response.json()) as Pick<
      KeycloakJwt,
      "name" | "preferred_username" | "email" | "picture"
    >;

    return extractBasicProfile(userInfo);
  } catch (error) {
    console.error("Error fetching user info:", error);
    return {};
  }
}
/**
 * How many milliseconds before expiry we proactively refresh the access token.
 * Refreshing slightly early reduces the window during which multiple concurrent
 * requests can all see an expired token and race to refresh it simultaneously.
 */
const REFRESH_BUFFER_MS = 60_000;

/**
 * In-process deduplication map for concurrent refresh calls.
 *
 * When multiple requests arrive at the same time and the token is expired,
 * the first one starts the refresh and stores its Promise here. Every
 * subsequent request for the **same** refresh token awaits the existing
 * Promise instead of issuing a second (doomed) Keycloak request.
 * Keycloak invalidates a refresh token on first use (token rotation), so any
 * parallel request that uses the same refresh token would receive
 * REFRESH_TOKEN_ERROR. The map entry is removed once the refresh settles.
 */
const refreshInProgress = new Map<string, Promise<JWT>>();

/**
 * Performs a single token-refresh request against Keycloak.
 * Not exported – callers should use refreshAccessToken() which adds
 * the in-process deduplication guard.
 */
async function doRefreshAccessToken(token: JWT): Promise<JWT> {
  const response = await fetch(
    `${process.env.AUTH_KEYCLOAK_ISSUER}/protocol/openid-connect/token`,
    {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: new URLSearchParams({
        client_id: process.env.AUTH_KEYCLOAK_ID!,
        client_secret: process.env.AUTH_KEYCLOAK_SECRET!,
        grant_type: "refresh_token",
        refresh_token: token.refreshToken!,
      }),
    }
  );

  if (!response.ok) {
    throw new Error("Failed to refresh access token");
  }

  const refreshedTokens = (await response.json()) as {
    access_token: string;
    expires_in: number;
    refresh_token?: string;
  };

  const refreshedClaims = extractProfileClaims(refreshedTokens.access_token);

  return mergeProfileClaims(
    {
      ...token,
      accessToken: refreshedTokens.access_token,
      accessTokenExpires: Date.now() + refreshedTokens.expires_in * 1000,
      refreshToken: refreshedTokens.refresh_token ?? token.refreshToken,
    },
    refreshedClaims
  );
}

/**
 * Refreshes an expired (or soon-to-expire) access token using the refresh
 * token. Concurrent calls for the same refresh token are deduplicated: only
 * one HTTP request is sent to Keycloak and all callers receive the result of
 * that single request.
 */
async function refreshAccessToken(token: JWT): Promise<JWT> {
  if (!token.refreshToken) {
    console.error("Missing refresh token");
    return { ...token, error: "RefreshAccessTokenError" };
  }

  const key = token.refreshToken;

  const existing = refreshInProgress.get(key);
  if (existing) {
    return existing;
  }

  const refreshPromise = doRefreshAccessToken(token)
    .catch((error: unknown) => {
      console.error("Error refreshing access token:", error);
      return { ...token, error: "RefreshAccessTokenError" } as JWT;
    })
    .finally(() => {
      refreshInProgress.delete(key);
    });

  // Register the in-flight Promise before any await so that concurrent calls
  // that arrive synchronously (before the first microtask tick) find it.
  refreshInProgress.set(key, refreshPromise);
  return refreshPromise;
}

export const authOptions: AuthOptions = {
  providers: [
    KeycloakProvider({
      clientId: process.env.AUTH_KEYCLOAK_ID!,
      clientSecret: process.env.AUTH_KEYCLOAK_SECRET!,
      issuer: process.env.AUTH_KEYCLOAK_ISSUER,
    }),
  ],
  callbacks: {
    async jwt({ token, account }) {
      // On initial sign-in, persist the tokens from Keycloak
      if (account) {
        if (!account.access_token) {
          console.error("Missing access_token on account");
          return { ...token, error: "RefreshAccessTokenError" };
        }

        const initialClaims = extractProfileClaims(account.access_token);

        return mergeProfileClaims(
          {
            ...token,
            accessToken: account.access_token,
            accessTokenExpires: (account.expires_at ?? 0) * 1000,
            refreshToken: account.refresh_token,
            idToken: account.id_token,
          },
          initialClaims
        );
      }

      // Return token if it is still valid with enough time to spare.
      // We refresh REFRESH_BUFFER_MS early to avoid a window where multiple
      // concurrent requests simultaneously see an expired token.
      if (Date.now() < (token.accessTokenExpires as number) - REFRESH_BUFFER_MS) {
        return token;
      }

      // Token has expired (or is about to) — refresh it.
      return refreshAccessToken(token);
    },
    async session({ session, token }) {
      // Access token is exposed on the session for use by the client-side
      // openapi-fetch wrapper (useApiClient) when calling the /api/backend
      // proxy. Roles are used for server-side authorization checks.
      session.accessToken = token.accessToken;
      session.roles = token.roles as string[];
      session.error = token.error;
      session.userId = token.sub;
      // Skip fetching live profile when the token is in an error state (e.g. refresh
      // failed because the refresh token expired or an admin terminated the session).
      // Calling userinfo with an expired/invalid access token would always return 401.
      const liveProfile =
        typeof token.accessToken === "string" && !token.error
          ? await fetchUserInfo(token.accessToken)
          : {};

      if (session.user) {
        session.user.name =
          liveProfile.name ?? (typeof token.name === "string" ? token.name : session.user.name);
        session.user.email =
          liveProfile.email ?? (typeof token.email === "string" ? token.email : session.user.email);
        session.user.image =
          liveProfile.picture ??
          (typeof token.picture === "string" ? token.picture : session.user.image);
      }

      return session;
    },
  },
};
