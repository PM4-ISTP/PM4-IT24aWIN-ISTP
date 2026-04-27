import { Alert, Box, Container, Group, Stack, Title } from "@mantine/core";
import { IconArrowLeft, IconBook2 } from "@tabler/icons-react";
import Link from "next/link";
import { CourseBannerHeader } from "@/src/features/course/components/course/CourseBannerHeader";
import { CourseChallengeDetailsList } from "@/src/features/course/components/management/CourseChallengeDetailsList";
import { CourseJourneyCard } from "@/src/features/course/components/course/CourseJourneyCard";
import { fetchPublicCourse } from "@/src/features/course/actions/courses";
import type { CourseDetailInstructorResponseDto } from "@/src/features/course/actions/courses";
import type { InstructorRoleEnum } from "@/src/shared/types/course";
import { getSanitizedHtml } from "@/src/shared/lib/utils";

const OWNER_ROLE: InstructorRoleEnum = "OWNER";

function getOwner(instructors: CourseDetailInstructorResponseDto[] | undefined) {
  let owner = undefined;
  if (instructors) {
    owner = instructors.find((ci) => ci.instructorRole === OWNER_ROLE);
  }
  return owner;
}

export default async function CourseDetails({
  courseId,
  backPageName,
  backHref,
}: {
  courseId: string;
  backPageName: string;
  backHref: string;
}) {
  const result = await fetchPublicCourse(courseId);

  if (!result.success || result.data.id === undefined) {
    // If ID is undefined, we cannot really do anything with the course anymore. Something is clearly very wrong.
    const errorMessage = result.success
      ? "Something failed during the loading of the course."
      : result.error;

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
            {errorMessage}
          </Alert>
        </Stack>
      </Container>
    );
  }

  const course = result.data;
  const title = course.title ?? "";
  const sanitizedDescription =
    course.description === undefined ? "" : getSanitizedHtml(course.description);
  const isEnrolled = course.isEnrolled ?? false;
  const participantCount = course.participantCount ?? 0;
  const isPublished = course.isPublished ?? false;
  const owner = getOwner(course.courseInstructors);

  return (
    <>
      <CourseBannerHeader
        title={title}
        topic={course.topic}
        shortDescription={course.shortDescription}
        description={sanitizedDescription}
        courseId={course.id!} // already checked, that ID is not undefined
        isEnrolled={isEnrolled}
        participantCount={participantCount}
        isPublished={isPublished}
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
            challenges={course.courseChallenges ?? []}
            title="Course Challenges"
            showIndex={true}
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
