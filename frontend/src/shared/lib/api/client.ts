"use client";

import { useMemo } from "react";
import { createApiClient } from ".";

export function useApiClient() {
  return useMemo(() => createApiClient("/api/backend"), []);
}
