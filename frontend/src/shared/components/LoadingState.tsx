"use client";

import { Center, Loader, Stack, Text } from "@mantine/core";
import type { ReactNode } from "react";

export interface LoadingStateProps {
  label?: ReactNode;
  size?: "xs" | "sm" | "md" | "lg" | "xl";
  minHeight?: number | string;
}

export function LoadingState({ label, size = "md", minHeight = 160 }: LoadingStateProps) {
  return (
    <Center mih={minHeight} p="lg">
      <Stack align="center" gap="sm">
        <Loader size={size} />
        {label ? (
          <Text size="sm" c="dimmed">
            {label}
          </Text>
        ) : null}
      </Stack>
    </Center>
  );
}
