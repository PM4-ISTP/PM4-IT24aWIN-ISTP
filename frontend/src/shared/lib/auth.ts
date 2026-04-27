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

      // Return token if it hasn't expired yet
      if (Date.now() < (token.accessTokenExpires as number)) {
        return token;
      }

      // Token has expired — refresh it
      return refreshAccessToken(token);
    },
    async session({ session, token }) {
      // Access token is intentionally NOT exposed to the client.
      // Use getServerSession() + fetchBackend() for backend calls.
      // Roles used for server-side authorization checks.
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
