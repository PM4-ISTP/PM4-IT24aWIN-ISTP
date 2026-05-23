"use client";

import { useState } from "react";
import { Modal, Stack, Text, Group } from "@mantine/core";
import { useRouter } from "next/navigation";
import { leaveCourse } from "@/src/features/course/actions/courses";
import AppButton from "@/src/shared/components/AppButton";

interface LeaveCourseButtonProps {
  courseId: string;
  onLeave?: () => void;
}

export function LeaveCourseButton({ courseId, onLeave }: LeaveCourseButtonProps) {
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
    onLeave?.();
    router.refresh();
  }

  return (
    <>
      <AppButton tone="danger" size="xs" onClick={() => setModalOpen(true)}>
        Leave Course
      </AppButton>

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
            <AppButton
              tone="ghost"
              onClick={() => {
                setModalOpen(false);
                setError(null);
              }}
              disabled={isSubmitting}
            >
              Cancel
            </AppButton>
            <AppButton
              tone="danger"
              loading={isSubmitting}
              disabled={isSubmitting}
              onClick={() => void handleLeave()}
            >
              Leave Course
            </AppButton>
          </Group>
        </Stack>
      </Modal>
    </>
  );
}
