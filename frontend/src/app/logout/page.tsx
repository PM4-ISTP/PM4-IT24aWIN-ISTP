"use client";

import { useEffect } from "react";

export default function LogoutPage() {
  useEffect(() => {
    window.location.assign("/api/auth/logout");
  }, []);

  return null;
}
