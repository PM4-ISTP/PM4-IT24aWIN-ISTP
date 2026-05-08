import NextAuth from "next-auth";
import { authOptions } from "@/src/shared/lib/auth";
import type { NextRequest } from "next/server";

type AppRouteHandler = (
  req: NextRequest,
  context: { params: Promise<{ nextauth: string[] }> }
) => ReturnType<typeof NextAuth>;

const handler = NextAuth(authOptions) as unknown as AppRouteHandler;
export { handler as GET, handler as POST };
