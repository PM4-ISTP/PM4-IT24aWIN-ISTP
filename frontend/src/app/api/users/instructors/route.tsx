import { fetchBackend } from "@/src/shared/lib/api";
import type { NextRequest } from "next/server";
import { NextResponse } from "next/server";

export async function GET(req: NextRequest) {
  const { searchParams } = req.nextUrl;

  const params = new URLSearchParams();
  const query = searchParams.get("query");
  const size = searchParams.get("size") ?? "20";
  const page = searchParams.get("page") ?? "0";

  if (query) params.set("query", query);
  params.set("size", size);
  params.set("page", page);

  try {
    const res = await fetchBackend(`/api/v1/users/collaborators?${params}`);
    const body = await res.text();
    const contentType = res.headers.get("content-type");

    return new NextResponse(body, {
      status: res.status,
      headers: contentType ? { "content-type": contentType } : undefined,
    });
  } catch (error) {
    if (error instanceof Error && error.message === "Not authenticated") {
      return NextResponse.json({ error: "Not authenticated" }, { status: 401 });
    }

    return NextResponse.json({ error: "Failed to fetch collaborators" }, { status: 500 });
  }
}
