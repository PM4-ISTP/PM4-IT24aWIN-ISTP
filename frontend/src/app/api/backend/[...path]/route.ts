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
    proxyHeaders.set("Authorization", `Bearer ${token}`);
    proxyHeaders.set("Content-Type", "application/json");

    return fetch(targetUrl, {
      method: req.method,
      headers: proxyHeaders,
      body,
    });
  };

  let response = await fetchBackend(accessToken);

  // Fallback for race conditions (clock skew/revocation edge cases): force one
  // refresh and retry the request a single time.
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
