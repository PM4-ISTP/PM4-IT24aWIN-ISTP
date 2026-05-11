"use client";

import { useCallback } from "react";
import { signIn } from "next-auth/react";

export default function useSignInToDashboard() {
  return useCallback(() => {
    void signIn("keycloak", { callbackUrl: "/dashboard" });
  }, []);
}
