"use client";

import { startTransition, useState } from "react";
import { Alert, Flex, Group, Text } from "@mantine/core";
import { IconArrowRight } from "@tabler/icons-react";
import { useRouter } from "next/navigation";
import { useApiClient } from "@/src/shared/lib/api/client";
import AppButton from "@/src/shared/components/AppButton";
import { LeaveCourseButton } from "./LeaveCourseButton";
import type { CourseVisibility } from "@/src/shared/types/course";

interface CourseEnrollmentButtonProps {
  courseId: string;
  isEnrolled: boolean;
  participantCount: number;
  status: CourseVisibility;
  nextChallengeHref?: string;
}

export function CourseEnrollmentButton({
  courseId,
  isEnrolled,
  participantCount,
  status,
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
  if (status === "DRAFT") {
    return null;
  }

  return (
    <Flex direction="column" gap="xs" align={{ base: "flex-start", sm: "flex-end" }}>
      <Group gap="sm">
        {hasJoined ? (
          <AppButton
            size="md"
            rightSection={<IconArrowRight size={16} />}
            disabled={!nextChallengeHref}
            onClick={() => {
              if (nextChallengeHref) {
                router.push(nextChallengeHref);
              }
            }}
            data-testid="course-enrollment-action"
          >
            {nextChallengeHref ? "Continue Course" : "All Labs Completed"}
          </AppButton>
        ) : (
          <AppButton
            size="md"
            loading={isSubmitting}
            disabled={isSubmitting}
            onClick={() => {
              void handleEnroll();
            }}
            data-testid="course-enrollment-action"
          >
            Enroll in Course
          </AppButton>
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
    </Flex>
  );
}
