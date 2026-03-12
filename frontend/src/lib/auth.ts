import type { AuthOptions } from "next-auth";
import type { JWT } from "next-auth/jwt";
import KeycloakProvider from "next-auth/providers/keycloak";

/**
 * Refreshes an expired access token using the refresh token.
 */
async function refreshAccessToken(token: JWT): Promise<JWT> {
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
          refresh_token: token.refreshToken as string,
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
        return {
          ...token,
          accessToken: account.access_token,
          accessTokenExpires: (account.expires_at ?? 0) * 1000,
          refreshToken: account.refresh_token,
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
      session.error = token.error;
      return session;
    },
  },
};
