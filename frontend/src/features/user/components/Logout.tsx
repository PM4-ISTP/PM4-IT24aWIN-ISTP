"use client";
import Link from "next/link";
export default function Logout() {
  return (
    <Link href="/logout" prefetch={false}>
      Sign out
    </Link>
  );
}
