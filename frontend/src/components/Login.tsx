"use client";
import { signIn } from "next-auth/react";
import { Button } from "@mantine/core";

export default function Login() {
  return (
    <Button fullWidth onClick={() => void signIn("keycloak", { callbackUrl: "/dashboard" })}>
      Sign in with Keycloak
    </Button>
  );
}
