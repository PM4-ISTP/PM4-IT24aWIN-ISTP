"use client";
import Link from "next/link";
export default function Logout() {
  return (
    <Link href="/api/auth/logout" prefetch={false}>
      Sign out
    </Link>
  );
}
