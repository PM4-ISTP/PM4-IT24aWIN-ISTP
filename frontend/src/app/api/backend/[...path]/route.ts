import { getToken } from "next-auth/jwt";
import { cookies, headers } from "next/headers";
import type { GetTokenParams } from "next-auth/jwt";
import type { NextRequest } from "next/server";

const BACKEND_URL = process.env.BACKEND_URL ?? "http://localhost:8080";

async function proxy(req: NextRequest, params: { path: string[] }): Promise<Response> {
  const req_ = {
    headers: Object.fromEntries((await headers()).entries()),
    cookies: Object.fromEntries((await cookies()).getAll().map((c) => [c.name, c.value])),
  };

  const token = await getToken({
    req: req_ as GetTokenParams["req"],
    secret: process.env.NEXTAUTH_SECRET,
  });

  if (!token?.accessToken) {
    return new Response(JSON.stringify({ error: "Not authenticated" }), {
      status: 401,
      headers: { "Content-Type": "application/json" },
    });
  }

  const { path } = params;
  const targetPath = "/" + path.join("/");
  const search = req.nextUrl.search;
  const targetUrl = `${BACKEND_URL}${targetPath}${search}`;

  const proxyHeaders = new Headers();
  proxyHeaders.set("Authorization", `Bearer ${token.accessToken}`);
  proxyHeaders.set("Content-Type", "application/json");

  const body = req.method !== "GET" && req.method !== "HEAD" ? await req.text() : undefined;

  return fetch(targetUrl, {
    method: req.method,
    headers: proxyHeaders,
    body,
  });
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
