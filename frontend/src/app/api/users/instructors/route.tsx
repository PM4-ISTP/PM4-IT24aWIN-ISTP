import {fetchBackend} from "@/src/lib/api";
import type {NextRequest} from "next/server";
import {NextResponse} from "next/server";

export async function GET(req: NextRequest) {
    const {searchParams} = req.nextUrl;

    const params = new URLSearchParams();
    const name = searchParams.get("name");
    const size = searchParams.get("size") ?? "20";
    const page = searchParams.get("page") ?? "0";

    if (name) params.set("name", name);
    params.set("size", size);
    params.set("page", page);

    try {
        const res = await fetchBackend(`/api/v1/users/instructors?${params}`);
        const data: unknown = await res.json();
        return NextResponse.json(data);
    } catch {
        return NextResponse.json({error: "Not authenticated"}, {status: 401});
    }
}
