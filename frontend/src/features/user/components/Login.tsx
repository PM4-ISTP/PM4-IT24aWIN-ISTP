"use client";
import { signIn } from "next-auth/react";
import { Button } from "@mantine/core";

export default function Login() {
  return (
    <Button
      fullWidth
      size="md"
      radius="md"
      onClick={() => void signIn("keycloak", { callbackUrl: "/dashboard" })}
      style={{
        background: "linear-gradient(90deg, #2563eb, #4f46e5)",
        border: "none",
        fontFamily: "var(--font-space-grotesk), sans-serif",
        fontWeight: 600,
        letterSpacing: "0.02em",
        boxShadow: "0 2px 12px rgba(79,70,229,0.3)",
      }}
    >
      Sign in with Keycloak
    </Button>
  );
}
