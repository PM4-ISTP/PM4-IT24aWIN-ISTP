"use client";

import { Center, Stack, Text } from "@mantine/core";
import type { ReactNode } from "react";

export interface EmptyStateProps {
  icon?: ReactNode;
  title: string;
  message?: ReactNode;
  action?: ReactNode;
  minHeight?: number | string;
}

export function EmptyState({ icon, title, message, action, minHeight = 160 }: EmptyStateProps) {
  return (
    <Center mih={minHeight} p="lg">
      <Stack align="center" gap="xs">
        {icon}
        <Text fw={600} size="md" c="dark.0">
          {title}
        </Text>
        {message ? (
          <Text size="sm" c="dimmed" ta="center">
            {message}
          </Text>
        ) : null}
        {action}
      </Stack>
    </Center>
  );
}
