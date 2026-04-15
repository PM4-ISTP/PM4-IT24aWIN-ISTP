"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Alert, Button, Group, Modal, Stack, Text, TextInput } from "@mantine/core";
import { joinCourseByCode } from "@/src/lib/actions/courses";

interface JoinCourseModalProps {
  opened: boolean;
  onClose: () => void;
}

export default function JoinCourseModal({ opened, onClose }: JoinCourseModalProps) {
  const router = useRouter();
  const [code, setCode] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  function handleClose() {
    setCode("");
    setError(null);
    onClose();
  }

  async function handleSubmit() {
    const normalized = code.trim().toUpperCase();
    if (normalized.length !== 6) {
      setError("Please enter a 6-character invite code.");
      return;
    }

    setError(null);
    setIsSubmitting(true);

    const result = await joinCourseByCode(normalized);

    setIsSubmitting(false);

    if (!result.success) {
      const statusStr = result.error?.split(":")[0]?.trim() ?? "";
      const status = /^\d+$/.test(statusStr) ? parseInt(statusStr, 10) : NaN;
      if (status === 404 || status === 400) {
        setError("Invalid or expired invite code. Please check and try again.");
      } else {
        setError("Something went wrong. Please try again later.");
      }
      return;
    }

    handleClose();
    router.push(`/dashboard/catalog/${result.data.id}`);
  }

  return (
    <Modal
      opened={opened}
      onClose={handleClose}
      title={
        <Text fw={700} size="lg" style={{ color: "#f1f5f9" }}>
          Join a Course
        </Text>
      }
      centered
      styles={{
        content: {
          background: "#0f1729",
          border: "1px solid rgba(255,255,255,0.08)",
        },
        header: {
          background: "#0f1729",
          borderBottom: "1px solid rgba(255,255,255,0.06)",
        },
        close: { color: "#94a3b8" },
      }}
    >
      <Stack gap="md" pt="xs">
        <Text size="sm" style={{ color: "#94a3b8" }}>
          Enter the 6-character invite code provided by your instructor.
        </Text>

        <TextInput
          placeholder="ABC123"
          value={code}
          onChange={(e) => {
            setCode(
              e.currentTarget.value
                .toUpperCase()
                .replace(/[^A-Z0-9]/g, "")
                .slice(0, 6)
            );
            if (error) setError(null);
          }}
          onKeyDown={(e) => {
            if (e.key === "Enter") void handleSubmit();
          }}
          maxLength={6}
          error={error}
          autoFocus
          styles={{
            input: {
              background: "rgba(255,255,255,0.06)",
              border: "1px solid rgba(255,255,255,0.12)",
              color: "#f1f5f9",
              letterSpacing: "0.25em",
              fontSize: "1.25rem",
              fontWeight: 700,
              textAlign: "center",
              fontFamily: "var(--font-space-grotesk), monospace",
            },
          }}
        />

        {error && (
          <Alert color="red" variant="light">
            {error}
          </Alert>
        )}

        <Group justify="flex-end" gap="sm">
          <Button
            variant="outline"
            radius="md"
            onClick={handleClose}
            disabled={isSubmitting}
            style={{
              borderColor: "rgba(255,255,255,0.12)",
              color: "#e2e8f0",
              background: "rgba(255,255,255,0.04)",
            }}
          >
            Cancel
          </Button>
          <Button
            radius="md"
            loading={isSubmitting}
            disabled={isSubmitting || code.length !== 6}
            onClick={() => void handleSubmit()}
            style={{
              background: "linear-gradient(90deg, #2563eb, #4f46e5)",
              border: "none",
              fontWeight: 600,
            }}
          >
            Join Course
          </Button>
        </Group>
      </Stack>
    </Modal>
  );
}
