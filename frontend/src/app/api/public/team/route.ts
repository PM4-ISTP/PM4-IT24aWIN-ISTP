import { NextResponse } from "next/server";

export async function GET() {
  try {
    const backendUrl = process.env.BACKEND_URL || "http://localhost:8080";
    const res = await fetch(`${backendUrl}/api/v1/public/team`, {
      cache: "no-store",
    });
    if (!res.ok) return NextResponse.json([]);
    const data: unknown = await res.json();
    return NextResponse.json(data);
  } catch {
    return NextResponse.json([]);
  }
}