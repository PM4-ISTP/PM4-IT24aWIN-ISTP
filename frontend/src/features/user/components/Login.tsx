"use client";
import { signIn } from "next-auth/react";
import AppButton from "@/src/shared/components/AppButton";

export default function Login() {
  return (
    <AppButton
      fullWidth
      size="md"
      onClick={() => void signIn("keycloak", { callbackUrl: "/dashboard" })}
    >
      Sign in with Keycloak
    </AppButton>
  );
}
