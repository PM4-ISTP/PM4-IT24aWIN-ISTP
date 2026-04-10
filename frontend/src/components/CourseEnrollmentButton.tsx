"use client";

import { startTransition, useState } from "react";
import { Alert, Button, Stack, Text } from "@mantine/core";
import { useRouter } from "next/navigation";

interface CourseEnrollmentButtonProps {
  courseId: string;
  isEnrolled: boolean;
  participantCount: number;
  isInstructor: boolean;
  isPublished: boolean;
}

function extractErrorMessage(text: string, fallback: string): string {
  if (!text) {
    return fallback;
  }

  try {
    const parsed = JSON.parse(text) as { error?: unknown };
    if (typeof parsed.error === "string" && parsed.error.trim()) {
      return parsed.error;
    }
  } catch {
    return text || fallback;
  }

  return text || fallback;
}

export function CourseEnrollmentButton({
  courseId,
  isEnrolled,
  participantCount,
  isInstructor,
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
      const response = await fetch(`/api/backend/api/v1/courses/catalog/${courseId}/enroll`, {
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

  if (!isPublished || isInstructor) {
    return null;
  }

  return (
    <Stack gap="xs" align="flex-end">
      <Button
        size="md"
        radius="md"
        color="blue"
        loading={isSubmitting}
        disabled={isSubmitting || hasJoined}
        onClick={() => {
          void handleEnroll();
        }}
      >
        {hasJoined ? "Enrolled" : "Enroll in Course"}
      </Button>
      <Text size="xs" style={{ color: "rgba(255,255,255,0.65)" }}>
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
