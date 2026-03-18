import { withAuth } from "next-auth/middleware";
import { NextResponse } from "next/server";
import { ROLE_GROUPS } from "./lib/roles";
import { isStringArray } from "./lib/utils";

const ROUTE_ROLES: Record<string, readonly string[]> = {
  "/dashboard/admin": ROLE_GROUPS.ADMIN_ONLY,
  "/dashboard/instructor": ROLE_GROUPS.INSTRUCTOR,
  "/dashboard": ROLE_GROUPS.ALL,
};

function getRequiredRoles(pathname: string): readonly string[] | null {
  const match = Object.keys(ROUTE_ROLES)
    .sort((a, b) => b.length - a.length)
    .find((route) => pathname.startsWith(route));

  return match ? ROUTE_ROLES[match] : null;
}

export default withAuth(
  function middleware(req) {
    const { pathname } = req.nextUrl;
    const token = req.nextauth.token;

    if (token?.error === "RefreshAccessTokenError") {
      const signOutUrl = new URL("/api/auth/signout", req.url);
      signOutUrl.searchParams.set("callbackUrl", "/");
      return NextResponse.redirect(signOutUrl);
    }

    const requiredRoles = getRequiredRoles(pathname);
    if (!requiredRoles) return NextResponse.next();

    const rawRoles = token?.roles;
    if (rawRoles !== undefined && !isStringArray(rawRoles)) {
      console.error("Unexpected token.roles shape:", rawRoles);
    }
    const userRoles = isStringArray(rawRoles) ? rawRoles : [];
    const hasRole = requiredRoles.some((role) => userRoles.includes(role));

    if (!hasRole) {
      return NextResponse.redirect(new URL("/unauthorized", req.url));
    }

    return NextResponse.next();
  },
  {
    callbacks: {
      authorized: ({ token }) => !!token,
    },
    pages: {
      signIn: "/",
    },
  }
);

export const config = {
  matcher: ["/dashboard/:path*"],
};
