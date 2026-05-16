"use client";

import { Alert, Button, Group, Stack, Text } from "@mantine/core";
import { SurfaceCard } from "@/src/shared/components/SurfaceCard";

export function CourseInviteCodePanel({
  inviteCode,
  codeCopied,
  regenerateError,
  isRegenerating,
  onCopyCode,
  onRegenerate,
}: {
  inviteCode: string | null;
  codeCopied: boolean;
  regenerateError: string | null;
  isRegenerating: boolean;
  onCopyCode: () => void;
  onRegenerate: () => void;
}) {
  return (
    <SurfaceCard variant="strong" elevation="md" padding="1.5rem">
      <Stack gap="sm">
        <Text
          size="sm"
          fw={600}
          style={{
            color: "#94a3b8",
            textTransform: "uppercase",
            letterSpacing: "0.08em",
            fontSize: "0.7rem",
          }}
        >
          Invite Code
        </Text>

        <Group gap="xs" align="center">
          <Text
            style={{
              fontFamily: "var(--font-space-grotesk), monospace",
              fontSize: "1.6rem",
              fontWeight: 700,
              letterSpacing: "0.3em",
              color: "#f1f5f9",
              lineHeight: 1,
            }}
          >
            {inviteCode ?? "—"}
          </Text>
          {inviteCode && (
            <Button
              variant="subtle"
              size="xs"
              radius="md"
              onClick={onCopyCode}
              style={{ color: codeCopied ? "#4ade80" : "#94a3b8" }}
              leftSection={
                <span
                  className="material-symbols-outlined"
                  style={{
                    fontSize: "0.95rem",
                    lineHeight: 1,
                    fontVariationSettings: "'FILL' 0, 'wght' 300, 'GRAD' 0, 'opsz' 24",
                  }}
                >
                  {codeCopied ? "check" : "content_copy"}
                </span>
              }
            >
              {codeCopied ? "Copied!" : "Copy"}
            </Button>
          )}
        </Group>

        <Text size="xs" style={{ color: "#64748b" }}>
          Share this code with students to let them join the course directly.
        </Text>

        {regenerateError && (
          <Alert color="red" variant="light" py="xs">
            {regenerateError}
          </Alert>
        )}

        <Button
          variant="outline"
          size="xs"
          radius="md"
          loading={isRegenerating}
          disabled={isRegenerating}
          onClick={onRegenerate}
          leftSection={
            <span
              className="material-symbols-outlined"
              style={{
                fontSize: "0.95rem",
                lineHeight: 1,
                fontVariationSettings: "'FILL' 0, 'wght' 300, 'GRAD' 0, 'opsz' 24",
              }}
            >
              refresh
            </span>
          }
          style={{
            borderColor: "rgba(255,255,255,0.12)",
            color: "#e2e8f0",
            background: "rgba(255,255,255,0.04)",
            alignSelf: "flex-start",
          }}
        >
          Regenerate code
        </Button>
      </Stack>
    </SurfaceCard>
  );
}
