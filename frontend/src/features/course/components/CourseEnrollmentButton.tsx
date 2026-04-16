"use client";

import { startTransition, useState } from "react";
import { Alert, Button, Group, Stack, Text } from "@mantine/core";
import { IconArrowRight } from "@tabler/icons-react";
import { useRouter } from "next/navigation";
import { extractErrorMessage } from "@/src/shared/lib/utils";

const CATALOG_ENROLL_API = (courseId: string) =>
  `/api/backend/api/v1/courses/catalog/${courseId}/enroll`;

interface CourseEnrollmentButtonProps {
  courseId: string;
  isEnrolled: boolean;
  participantCount: number;
  isPublished: boolean;
}

export function CourseEnrollmentButton({
  courseId,
  isEnrolled,
  participantCount,
  isPublished,
}: CourseEnrollmentButtonProps) {
  const router = useRouter();
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [joinError, setJoinError] = useState<string | null>(null);
  const [hasJoined, setHasJoined] = useState(isEnrolled);
  const [currentParticipantCount, setCurrentParticipantCount] = useState(participantCount);

  async function handleEnroll() {
    setIsSubmitting(true);
    setJoinError(null);

    try {
      const response = await fetch(CATALOG_ENROLL_API(courseId), {
        method: "POST",
      });

      if (!response.ok) {
        const message = extractErrorMessage(await response.text(), response.statusText);
        setJoinError(`${response.status}: ${message}`);
        return;
      }

      const updatedCourse = (await response.json()) as {
        isEnrolled?: boolean;
        participantCount?: number;
      };

      setHasJoined(Boolean(updatedCourse.isEnrolled));
      if (typeof updatedCourse.participantCount === "number") {
        setCurrentParticipantCount(updatedCourse.participantCount);
      } else {
        setCurrentParticipantCount((count) => count + 1);
      }
      startTransition(() => {
        router.refresh();
      });
    } catch (error) {
      setJoinError(error instanceof Error ? error.message : "Failed to enroll");
    } finally {
      setIsSubmitting(false);
    }
  }

  if (!isPublished) {
    return null;
  }

  return (
    <Stack gap="xs" align="flex-end">
      <Group gap="sm">
        {hasJoined ? (
          /* TODO: Replace href with real lesson route once lessons are implemented */
          <Button
            size="md"
            radius="md"
            rightSection={<IconArrowRight size={16} />}
            onClick={() => router.push(`/dashboard/learn/${courseId}`)}
            style={{
              background: "linear-gradient(90deg, #2563eb, #4f46e5)",
              border: "none",
              fontFamily: "var(--font-space-grotesk), sans-serif",
              fontWeight: 600,
              boxShadow: "0 2px 12px rgba(79,70,229,0.3)",
            }}
          >
            Start Next Lesson
          </Button>
        ) : (
          <Button
            size="md"
            radius="md"
            loading={isSubmitting}
            disabled={isSubmitting}
            onClick={() => {
              void handleEnroll();
            }}
            style={{
              background: "linear-gradient(90deg, #2563eb, #4f46e5)",
              border: "none",
              fontFamily: "var(--font-space-grotesk), sans-serif",
              fontWeight: 600,
              boxShadow: "0 2px 12px rgba(79,70,229,0.3)",
            }}
          >
            Enroll in Course
          </Button>
        )}
      </Group>
      <Text size="xs" c="dimmed">
        {currentParticipantCount} participant{currentParticipantCount === 1 ? "" : "s"}
      </Text>
      {joinError && (
        <Alert color="red" variant="light" title="Enrollment failed">
          {joinError}
        </Alert>
      )}
    </Stack>
  );
}
