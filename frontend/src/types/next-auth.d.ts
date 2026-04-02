import "next-auth";
import "next-auth/jwt";

declare module "next-auth" {
  interface Session {
    accessToken?: string;
    roles?: string[];
    error?: string;
    userId?: string;
  }
}

declare module "next-auth/jwt" {
  interface JWT {
    accessToken?: string;
    accessTokenExpires?: number;
    refreshToken?: string;
    roles?: string[];
    error?: string;
  }
}

export type KeycloakJwt = {
  sub?: string;
  realm_access?: { roles?: string[] };
  resource_access?: Record<string, { roles?: string[] }>;
  name?: string;
  preferred_username?: string;
  email?: string;
  picture?: string;
};
