import { cookies, headers } from "next/headers";
import type { GetTokenParams } from "next-auth/jwt";
import type { NextRequest } from "next/server";
import { getValidAccessToken } from "@/src/shared/lib/api/server";

const BACKEND_URL = process.env.BACKEND_URL ?? "http://localhost:8080";

async function proxy(req: NextRequest, params: { path: string[] }): Promise<Response> {
  const req_ = {
    headers: Object.fromEntries((await headers()).entries()),
    cookies: Object.fromEntries((await cookies()).getAll().map((c) => [c.name, c.value])),
  };

  const authReq = req_ as GetTokenParams["req"];
  let accessToken: string;
  try {
    accessToken = await getValidAccessToken(authReq);
  } catch {
    return new Response(JSON.stringify({ error: "Not authenticated" }), {
      status: 401,
      headers: { "Content-Type": "application/json" },
    });
  }

  const { path } = params;
  const targetPath = "/" + path.join("/");
  const search = req.nextUrl.search;
  const targetUrl = `${BACKEND_URL}${targetPath}${search}`;

  const body = req.method !== "GET" && req.method !== "HEAD" ? await req.text() : undefined;
  const fetchBackend = (token: string) => {
    const proxyHeaders = new Headers();
    const accept = req.headers.get("accept");
    const forwardedHost = req.headers.get("x-forwarded-host") ?? req.headers.get("host");
    const forwardedProto =
      req.headers.get("x-forwarded-proto") ?? req.nextUrl.protocol.replace(":", "");

    proxyHeaders.set("Authorization", `Bearer ${token}`);
    proxyHeaders.set("Content-Type", "application/json");
    if (accept) proxyHeaders.set("Accept", accept);
    if (forwardedHost) proxyHeaders.set("X-Forwarded-Host", forwardedHost);
    if (forwardedProto) proxyHeaders.set("X-Forwarded-Proto", forwardedProto);

    return fetch(targetUrl, {
      method: req.method,
      headers: proxyHeaders,
      body,
    });
  };

  let response = await fetchBackend(accessToken);

  // Fallback for timing gaps where the token passes local expiry checks but is
  // already rejected by backend/IdP (e.g. revocation, issuer clock skew, or
  // token becoming invalid between validation and the upstream request). Force
  // one refresh and retry a single time.
  if (response.status === 401) {
    try {
      const refreshedAccessToken = await getValidAccessToken(authReq, { forceRefresh: true });
      response = await fetchBackend(refreshedAccessToken);
    } catch {
      // Keep original 401 response from backend.
    }
  }

  return response;
}

export const GET = (req: NextRequest, { params }: { params: Promise<{ path: string[] }> }) =>
  params.then((p) => proxy(req, p));

export const POST = (req: NextRequest, { params }: { params: Promise<{ path: string[] }> }) =>
  params.then((p) => proxy(req, p));

export const PUT = (req: NextRequest, { params }: { params: Promise<{ path: string[] }> }) =>
  params.then((p) => proxy(req, p));

export const PATCH = (req: NextRequest, { params }: { params: Promise<{ path: string[] }> }) =>
  params.then((p) => proxy(req, p));

export const DELETE = (req: NextRequest, { params }: { params: Promise<{ path: string[] }> }) =>
  params.then((p) => proxy(req, p));
