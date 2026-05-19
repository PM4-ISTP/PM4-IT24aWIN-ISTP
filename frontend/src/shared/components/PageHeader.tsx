import { Group, Stack, Text, Title } from "@mantine/core";
import type { ReactNode } from "react";

interface PageHeaderProps {
  title: ReactNode;
  subtitle?: ReactNode;
  /** Optional action(s) rendered on the right (e.g. a primary button). */
  action?: ReactNode;
}

/**
 * Standard dashboard page header: title + subtitle, with an optional action
 * aligned to the right. Replaces the hand-rolled Title/Text blocks that were
 * duplicated across the dashboard pages.
 */
export default function PageHeader({ title, subtitle, action }: PageHeaderProps) {
  return (
    <Group justify="space-between" align="flex-end" wrap="nowrap">
      <Stack gap={4}>
        <Title
          order={1}
          size="h2"
          style={{
            color: "#f1f5f9",
            fontFamily: "var(--font-space-grotesk), sans-serif",
            fontWeight: 700,
          }}
        >
          {title}
        </Title>
        {subtitle ? (
          <Text size="sm" style={{ color: "#94a3b8" }}>
            {subtitle}
          </Text>
        ) : null}
      </Stack>
      {action ? <div style={{ flexShrink: 0 }}>{action}</div> : null}
    </Group>
  );
}
