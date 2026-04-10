import {
  Alert,
  Box,
  Container,
  Divider,
  Group,
  Paper,
  Stack,
  Text,
} from "@mantine/core";
import { getServerSession } from "next-auth";
import { IconArrowLeft, IconBook2 } from "@tabler/icons-react";
import Link from "next/link";
import { CourseBannerHeader } from "@/src/components/CourseBannerHeader";
import { CourseInstructorCard } from "@/src/components/CourseInstructorCard";
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
  const owner =
    course.courseInstructors.find((ci) => ci.instructorRole === OWNER_ROLE)?.instructor ?? null;
  const currentUserId = session?.userId ?? null;
  const isInstructor = currentUserId
    ? course.courseInstructors.some((ci) => ci.instructor.id === currentUserId)
    : false;

  return (
    <Box style={{ minHeight: "100vh", backgroundColor: "var(--mantine-color-gray-0)" }}>

      <CourseBannerHeader
        title={course.title}
        topic={course.topic}
        difficulty={course.difficulty}
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
      <Container size="lg" py="xl">
        <div style={{ display: "flex", gap: 24, alignItems: "flex-start", flexWrap: "wrap" }}>

          {/* Left column */}
          <div style={{ flex: "1 1 500px", minWidth: 0 }}>
            <Stack gap="lg">
              {/* About this course */}
              {course.description && (
                <Paper withBorder radius="lg" p="xl" shadow="xs">
                  <Stack gap="md">
                    <Group gap="sm" align="center">
                      <IconBook2 size={18} color="var(--mantine-color-blue-6)" />
                      <Text fw={700} size="lg">About this Course</Text>
                    </Group>
                    <Divider />
                    <div
                      className="course-description"
                      dangerouslySetInnerHTML={{ __html: course.description }}
                    />
                  </Stack>
                </Paper>
              )}
            </Stack>
          </div>

          {/* Right column – Instructor card */}
          <div style={{ flex: "0 0 280px", position: "sticky", top: 24 }}>
            {owner && <CourseInstructorCard instructor={owner} />}
          </div>

        </div>
      </Container>
    </Box>
  );
}
