import type { AuthOptions } from "next-auth";
import type { JWT } from "next-auth/jwt";
import KeycloakProvider from "next-auth/providers/keycloak";
import type { KeycloakJwt } from "../types/next-auth";
import { jwtDecode } from "jwt-decode";
/**
 * Refreshes an expired access token using the refresh token.
 */
async function refreshAccessToken(token: JWT): Promise<JWT> {
  if (!token.refreshToken) {
    console.error("Missing refresh token");
    return { ...token, error: "RefreshAccessTokenError" };
  }
  try {
    const response = await fetch(
      `${process.env.AUTH_KEYCLOAK_ISSUER}/protocol/openid-connect/token`,
      {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        body: new URLSearchParams({
          client_id: process.env.AUTH_KEYCLOAK_ID!,
          client_secret: process.env.AUTH_KEYCLOAK_SECRET!,
          grant_type: "refresh_token",
          refresh_token: token.refreshToken,
        }),
      }
    );

    const refreshedTokens = (await response.json()) as {
      access_token: string;
      expires_in: number;
      refresh_token?: string;
    };

    if (!response.ok) {
      throw new Error("Failed to refresh access token");
    }

    return {
      ...token,
      accessToken: refreshedTokens.access_token,
      accessTokenExpires: Date.now() + refreshedTokens.expires_in * 1000,
      refreshToken: refreshedTokens.refresh_token ?? token.refreshToken,
      // Decode the new access token to extract updated roles
      roles: jwtDecode<KeycloakJwt>(refreshedTokens.access_token).realm_access?.roles ?? [],
    };
  } catch (error) {
    console.error("Error refreshing access token:", error);
    return { ...token, error: "RefreshAccessTokenError" };
  }
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

        const decoded = jwtDecode<KeycloakJwt>(account.access_token);
        return {
          ...token,
          accessToken: account.access_token,
          accessTokenExpires: (account.expires_at ?? 0) * 1000,
          refreshToken: account.refresh_token,
          roles: decoded.realm_access?.roles ?? [],
        };
      }

      // Return token if it hasn't expired yet
      if (Date.now() < (token.accessTokenExpires as number)) {
        return token;
      }

      // Token has expired — refresh it
      return refreshAccessToken(token);
    },
    session({ session, token }) {
      // Access token is intentionally NOT exposed to the client.
      // Use getServerSession() + fetchBackend() for backend calls.
      // Roles used for server-side authorization checks.
      session.roles = token.roles as string[];
      session.error = token.error;
      return session;
    },
  },
};
