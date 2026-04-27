"use client";

import { useEffect, useRef } from "react";
import { useSession, signOut } from "next-auth/react";

/**
 * Watches the NextAuth session for token refresh errors (e.g. expired refresh
 * token or an admin-terminated session) and automatically signs the user out,
 * redirecting them to the home page.
 *
 * This component must be rendered inside a NextAuth SessionProvider, which is
 * already present in the root layout.
 */
export default function SessionErrorHandler() {
  const { data: session } = useSession();
  const isSigningOut = useRef(false);

  useEffect(() => {
    if (session?.error === "RefreshAccessTokenError" && !isSigningOut.current) {
      isSigningOut.current = true;
      void signOut({ callbackUrl: "/" });
    }
  }, [session?.error]);

  return null;
}
