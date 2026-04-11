import sanitizeHtml from "sanitize-html";
import { Alert, Container, Divider, Group, Paper, Stack, Text } from "@mantine/core";
import { getServerSession } from "next-auth";
import { IconArrowLeft, IconBook2 } from "@tabler/icons-react";
import Link from "next/link";
import { CourseBannerHeader } from "@/src/components/CourseBannerHeader";
import { CourseInstructorCard } from "@/src/components/CourseInstructorCard";
import { CourseJourneyCard } from "@/src/components/CourseJourneyCard";
import { fetchPublicCourse } from "@/src/lib/actions/courses";
import { authOptions } from "@/src/lib/auth";
import type { InstructorRoleEnum } from "@/src/types/course";

const OWNER_ROLE: InstructorRoleEnum = "OWNER";
const CATALOG_BACK_HREF = "/dashboard/catalog";

export default async function CatalogCoursePage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const session = await getServerSession(authOptions);
  const result = await fetchPublicCourse(id);

  if (!result.success) {
    return (
      <Container>
        <Stack p="xl" gap="lg">
          <Link href={CATALOG_BACK_HREF} style={{ textDecoration: "none" }}>
            <Group gap={6} style={{ color: "var(--mantine-color-dimmed)", fontSize: 14 }}>
              <IconArrowLeft size={16} />
              <span>Back to Catalog</span>
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
  const sanitizedDescription = course.description
    ? sanitizeHtml(course.description, {
        allowedTags: sanitizeHtml.defaults.allowedTags.concat(["img", "h1", "h2"]),
        allowedAttributes: {
          ...sanitizeHtml.defaults.allowedAttributes,
          img: ["src", "alt", "width", "height"],
        },
      })
    : null;
  const owner =
    course.courseInstructors.find((ci) => ci.instructorRole === OWNER_ROLE)?.instructor ?? null;
  const currentUserId = session?.userId ?? null;
  const isInstructor = currentUserId
    ? course.courseInstructors.some((ci) => ci.instructor.id === currentUserId)
    : false;

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
        isInstructor={isInstructor}
        isPublished={course.isPublished}
        backHref={CATALOG_BACK_HREF}
      />

      {/* Main content: two-column flex layout */}
      <Container size="lg" pt="md" pb="xl">
        <Stack gap="lg">
          <div style={{ display: "flex", gap: 24, alignItems: "stretch", flexWrap: "wrap" }}>
            {/* Left column */}
            <div style={{ flex: "1 1 500px", minWidth: 0 }}>
              {/*
               * Course Journey — lessons & challenges progress.
               *
               * TODO (lessons):    Pass `lessons={{ finished, total }}` once lesson-completion
               *                    tracking is implemented in the backend.
               * TODO (challenges): Pass `challenges={{ completed, total }}` once the challenges
               *                    feature is available. Until then both bars show as placeholders.
               */}
              <CourseJourneyCard
              // lessons={undefined}    ← wire up when lesson API is ready
              // challenges={undefined} ← wire up when challenge API is ready
              />
            </div>

            {/* Right column – Instructor card */}
            <div style={{ flex: "0 0 280px", alignSelf: "stretch" }}>
              {owner && (
                <div style={{ height: "100%" }}>
                  <CourseInstructorCard instructor={owner} />
                </div>
              )}
            </div>
          </div>

          {/* About this course */}
          {sanitizedDescription && (
            <Paper withBorder radius="lg" p="xl" shadow="xs">
              <Stack gap="md">
                <Group gap="sm" align="center">
                  <IconBook2 size={18} color="var(--mantine-color-blue-6)" />
                  <Text fw={700} size="lg">
                    About this Course
                  </Text>
                </Group>
                <Divider />
                <div
                  className="course-description"
                  dangerouslySetInnerHTML={{ __html: sanitizedDescription }}
                />
              </Stack>
            </Paper>
          )}
        </Stack>
      </Container>
    </>
  );
}
