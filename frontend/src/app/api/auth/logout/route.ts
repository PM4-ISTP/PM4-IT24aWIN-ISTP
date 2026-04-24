import { getToken } from "next-auth/jwt";
import type { NextRequest } from "next/server";
import { NextResponse } from "next/server";

const NEXT_AUTH_COOKIES = [
  "next-auth.session-token",
  "__Secure-next-auth.session-token",
  "next-auth.csrf-token",
  "__Host-next-auth.csrf-token",
  "next-auth.callback-url",
  "__Secure-next-auth.callback-url",
  "next-auth.pkce.code_verifier",
  "next-auth.state",
  "next-auth.nonce",
] as const;

function isHttpsRequest(request: NextRequest): boolean {
  const forwardedProto = request.headers.get("x-forwarded-proto");
  if (forwardedProto) {
    return forwardedProto.split(",")[0]?.trim().toLowerCase() === "https";
  }

  return request.nextUrl.protocol === "https:";
}

function clearNextAuthCookies(request: NextRequest, response: NextResponse): void {
  const isHttps = isHttpsRequest(request);

  for (const name of NEXT_AUTH_COOKIES) {
    const requiresSecurePrefix = name.startsWith("__Secure-") || name.startsWith("__Host-");
    const secure = isHttps || requiresSecurePrefix;

    response.cookies.set(name, "", {
      path: "/",
      maxAge: 0,
      secure,
      sameSite: "lax",
    });
  }
}

function normalizeBaseUrl(value: string | undefined): string | undefined {
  if (!value) return undefined;
  const trimmed = value.trim();
  if (!trimmed) return undefined;
  return trimmed.endsWith("/") ? trimmed.slice(0, -1) : trimmed;
}

function getPublicBaseUrl(request: NextRequest): string {
  const configured =
    normalizeBaseUrl(process.env.AUTH_POST_LOGOUT_REDIRECT_URI) ??
    normalizeBaseUrl(process.env.NEXTAUTH_URL);
  if (configured) {
    return configured;
  }

  const forwardedProto = request.headers.get("x-forwarded-proto");
  const forwardedHost = request.headers.get("x-forwarded-host");
  if (forwardedProto && forwardedHost) {
    return `${forwardedProto}://${forwardedHost}`;
  }

  return new URL(request.url).origin;
}

export async function GET(request: NextRequest) {
  const baseUrl = getPublicBaseUrl(request);
  const postLogoutRedirectUri = `${baseUrl}/`;

  const response = NextResponse.redirect(postLogoutRedirectUri, 302);
  clearNextAuthCookies(request, response);

  const issuer = process.env.AUTH_KEYCLOAK_ISSUER;
  const clientId = process.env.AUTH_KEYCLOAK_ID;
  const secret = process.env.NEXTAUTH_SECRET;

  if (!issuer || !clientId) {
    return response;
  }

  let idTokenHint: string | undefined;
  if (secret) {
    try {
      const token = await getToken({ req: request, secret });
      if (typeof token?.idToken === "string") {
        idTokenHint = token.idToken;
      }
    } catch {
      // Ignore and fall back to client_id-only logout if possible.
    }
  }

  const logoutUrl = new URL(`${issuer}/protocol/openid-connect/logout`);
  logoutUrl.searchParams.set("post_logout_redirect_uri", postLogoutRedirectUri);
  logoutUrl.searchParams.set("client_id", clientId);
  if (idTokenHint) {
    logoutUrl.searchParams.set("id_token_hint", idTokenHint);
  }

  response.headers.set("Location", logoutUrl.toString());
  return response;
}
