import { Alert, Box, Container, Group, Stack, Title } from "@mantine/core";
import { IconArrowLeft, IconBook2 } from "@tabler/icons-react";
import Link from "next/link";
import { CourseBannerHeader } from "@/src/components/CourseBannerHeader";
import { CourseChallengeDetailsList } from "@/src/components/CourseChallengeDetailsList";
import { CourseJourneyCard } from "@/src/components/CourseJourneyCard";
import { fetchChallenge } from "@/src/lib/actions/challenges";
import { fetchPublicCourse } from "@/src/lib/actions/courses";
import type { InstructorRoleEnum } from "@/src/types/course";
import { getSanitizedHtml } from "@/src/lib/utils";

const OWNER_ROLE: InstructorRoleEnum = "OWNER";

export default async function CourseDetails({
  userId,
  backPageName,
  backHref,
}: {
  userId: string;
  backPageName: string;
  backHref: string;
}) {
  const result = await fetchPublicCourse(userId);

  if (!result.success) {
    return (
      <Container>
        <Stack p="xl" gap="lg">
          <Link href={backHref} style={{ textDecoration: "none" }}>
            <Group gap={6} style={{ color: "rgba(255,255,255,0.45)", fontSize: 14 }}>
              <IconArrowLeft size={16} />
              <span>Back to {backPageName}</span>
            </Group>
          </Link>
          <Alert color="red" title="Failed to load course">
            {result.error}
          </Alert>
        </Stack>
      </Container>
    );
  }

  const course = result.data;
  const sanitizedDescription = course.description === null ? "" : getSanitizedHtml(course.description);
  const owner =
    course.courseInstructors.find((ci) => ci.instructorRole === OWNER_ROLE)?.instructor ?? null;
  const challengeIds = (course.courseChallenges ?? []).map((c) => c.challengeId);
  const challengeResults = await Promise.all(challengeIds.map((challengeId) => fetchChallenge(challengeId)));
  const challengeDetails = challengeResults.flatMap((result) => (result.success ? [result.data] : []));
  const failedChallengeCount = challengeResults.length - challengeDetails.length;

  return (
    <>
      <CourseBannerHeader
        title={course.title}
        topic={course.topic}
        shortDescription={course.shortDescription}
        description={course.description}
        courseId={course.id}
        isEnrolled={course.isEnrolled}
        participantCount={course.participantCount}
        isPublished={course.isPublished}
        backPageName={backPageName}
        backHref={backHref}
      />

      {/* Main content */}
      <Container size="lg" pt="md" pb="xl">
        <Stack gap="lg">
          <CourseJourneyCard
            instructor={owner}
            // lessons={undefined}    ← wire up when lesson API is ready
            // challenges={undefined} ← wire up when challenge API is ready
          />

          <CourseChallengeDetailsList
            challenges={challengeDetails}
            failedCount={failedChallengeCount}
          />

          {/* About this course */}
          {sanitizedDescription && (
            <Box
              style={{
                background: "rgba(255,255,255,0.04)",
                border: "1px solid rgba(255,255,255,0.08)",
                borderRadius: 14,
                padding: "2rem",
                boxShadow: "0 4px 24px rgba(0,0,0,0.25)",
              }}
            >
              <Stack gap="md">
                <Group gap="sm" align="center">
                  <IconBook2 size={18} color="#60a5fa" />
                  <Title
                    order={3}
                    style={{
                      color: "#f1f5f9",
                      fontFamily: "var(--font-space-grotesk), sans-serif",
                      fontWeight: 700,
                      fontSize: "1.1rem",
                    }}
                  >
                    About this Course
                  </Title>
                </Group>
                <div
                  style={{ borderTop: "1px solid rgba(255,255,255,0.08)", paddingTop: "1rem" }}
                  className="course-description"
                  dangerouslySetInnerHTML={{ __html: sanitizedDescription }}
                />
              </Stack>
            </Box>
          )}
        </Stack>
      </Container>
    </>
  );
}
