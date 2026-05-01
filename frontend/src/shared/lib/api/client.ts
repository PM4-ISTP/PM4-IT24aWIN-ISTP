"use client";

import { useSession } from "next-auth/react";
import { useMemo } from "react";
import { createApiClient } from ".";

export function useApiClient() {
  const { data: session } = useSession();
  return useMemo(
    () => createApiClient("/api/backend", (session as { accessToken?: string })?.accessToken ?? ""),
    [session]
  );
}
