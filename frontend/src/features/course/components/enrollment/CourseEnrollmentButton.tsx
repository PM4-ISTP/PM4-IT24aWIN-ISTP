"use client";

import { startTransition, useState } from "react";
import { Alert, Button, Group, Stack, Text } from "@mantine/core";
import { IconArrowRight } from "@tabler/icons-react";
import { useRouter } from "next/navigation";
import { useApiClient } from "@/src/shared/lib/api/client";
import { LeaveCourseButton } from "./LeaveCourseButton";

interface CourseEnrollmentButtonProps {
  courseId: string;
  isEnrolled: boolean;
  participantCount: number;
  isPublished: boolean;
  isPrivate?: boolean;
  nextChallengeHref?: string;
}

export function CourseEnrollmentButton({
  courseId,
  isEnrolled,
  participantCount,
  isPublished,
  isPrivate = false,
  nextChallengeHref,
}: CourseEnrollmentButtonProps) {
  const router = useRouter();
  const client = useApiClient();
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [joinError, setJoinError] = useState<string | null>(null);
  const [hasJoined, setHasJoined] = useState(isEnrolled);
  const [currentParticipantCount, setCurrentParticipantCount] = useState(participantCount);

  async function handleEnroll() {
    setIsSubmitting(true);
    setJoinError(null);

    try {
      const { data, error } = await client.POST("/api/v1/courses/catalog/{id}/enroll", {
        params: { path: { id: courseId } },
      });

      if (data === undefined || error !== undefined) {
        setJoinError(error?.error ?? "Cannot join course");
        return;
      }

      const updatedCourse = data as {
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

  // Show nothing only for draft courses (not published, not private).
  // Private courses: accessible via invite code — enroll button should still appear.
  if (!isPublished && !isPrivate) {
    return null;
  }

  return (
    <Stack gap="xs" align="flex-end">
      <Group gap="sm">
        {hasJoined ? (
          <Button
            size="md"
            radius="md"
            rightSection={<IconArrowRight size={16} />}
            disabled={!nextChallengeHref}
            onClick={() => {
              if (nextChallengeHref) {
                router.push(nextChallengeHref);
              }
            }}
            style={{
              background: "linear-gradient(90deg, #2563eb, #4f46e5)",
              border: "none",
              fontFamily: "var(--font-space-grotesk), sans-serif",
              fontWeight: 600,
              boxShadow: "0 2px 12px rgba(79,70,229,0.3)",
            }}
            data-testid="course-enrollment-action"
          >
            {nextChallengeHref ? "Continue Course" : "All Labs Completed"}
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
            data-testid="course-enrollment-action"
          >
            Enroll in Course
          </Button>
        )}
      </Group>
      <Text size="xs" c="dimmed">
        {currentParticipantCount} participant{currentParticipantCount === 1 ? "" : "s"}
      </Text>
      {hasJoined && (
        <LeaveCourseButton
          courseId={courseId}
          onLeave={() => {
            setHasJoined(false);
            setCurrentParticipantCount((c) => c - 1);
          }}
        />
      )}
      {joinError && (
        <Alert color="red" variant="light" title="Enrollment failed">
          {joinError}
        </Alert>
      )}
    </Stack>
  );
}
