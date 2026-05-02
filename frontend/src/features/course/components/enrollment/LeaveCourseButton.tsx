"use client";

import { useState } from "react";
import { Button, Modal, Stack, Text, Group } from "@mantine/core";
import { useRouter } from "next/navigation";
import { leaveCourse } from "@/src/features/course/actions/courses";

interface LeaveCourseButtonProps {
  courseId: string;
}

export function LeaveCourseButton({ courseId }: LeaveCourseButtonProps) {
  const router = useRouter();
  const [modalOpen, setModalOpen] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleLeave() {
    setIsSubmitting(true);
    setError(null);

    const result = await leaveCourse(courseId);

    setIsSubmitting(false);

    if (!result.success) {
      setError("Something went wrong. Please try again.");
      return;
    }

    setModalOpen(false);
    router.refresh();
  }

  return (
    <>
      <Button
        variant="subtle"
        color="red"
        size="xs"
        onClick={() => setModalOpen(true)}
        style={{
          fontFamily: "var(--font-space-grotesk), sans-serif",
          fontWeight: 600,
        }}
      >
        Leave Course
      </Button>

      <Modal
        opened={modalOpen}
        onClose={() => {
          setModalOpen(false);
          setError(null);
        }}
        title={
          <Text fw={700} size="lg" style={{ color: "#f1f5f9" }}>
            Leave Course
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
            Are you sure you want to leave this course? Your progress will be saved and you can
            re-enroll at any time.
          </Text>

          {error && (
            <Text size="sm" c="red">
              {error}
            </Text>
          )}

          <Group justify="flex-end" gap="sm">
            <Button
              variant="outline"
              radius="md"
              onClick={() => {
                setModalOpen(false);
                setError(null);
              }}
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
              color="red"
              loading={isSubmitting}
              disabled={isSubmitting}
              onClick={() => void handleLeave()}
            >
              Leave Course
            </Button>
          </Group>
        </Stack>
      </Modal>
    </>
  );
}
