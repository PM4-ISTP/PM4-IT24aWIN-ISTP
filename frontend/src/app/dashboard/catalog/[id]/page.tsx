import {
  Alert,
  Avatar,
  Badge,
  Box,
  Container,
  Divider,
  Group,
  Paper,
  Progress,
  Stack,
  Text,
  Title,
} from "@mantine/core";
import { getServerSession } from "next-auth";
import {
  IconArrowLeft,
  IconBook2,
  IconCheck,
  IconCircleDashed,
  IconUser,
} from "@tabler/icons-react";
import Link from "next/link";
import { CourseEnrollmentButton } from "@/src/components/CourseEnrollmentButton";
import { fetchPublicCourse } from "@/src/lib/actions/courses";
import { authOptions } from "@/src/lib/auth";
import { getCoursePreviewText } from "@/src/lib/courseText";
import { difficultyColor, difficultyLabel } from "@/src/lib/courseUtils";

function getInitials(name: string): string {
  const parts = name.trim().split(/\s+/).filter(Boolean);
  if (parts.length === 0) return "?";
  return parts
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase() ?? "")
    .join("");
}

export default async function CatalogCoursePage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const session = await getServerSession(authOptions);
  const result = await fetchPublicCourse(id);

  if (!result.success) {
    return (
      <Container>
        <Stack p="xl" gap="lg">
          <Link href="/dashboard/catalog" style={{ textDecoration: "none" }}>
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
    course.courseInstructors.find((ci) => ci.instructorRole === "OWNER")?.instructor ?? null;
  const currentUserId = session?.userId ?? null;
  const isInstructor = currentUserId
    ? course.courseInstructors.some((ci) => ci.instructor.id === currentUserId)
    : false;

  const previewText = getCoursePreviewText(course.shortDescription, course.description);

  return (
    <Box style={{ minHeight: "100vh", backgroundColor: "var(--mantine-color-gray-0)" }}>

      {/* ── Header Banner ── */}
      <Box style={{
        background: "linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%)",
        padding: "48px 0 40px",
        position: "relative",
        overflow: "hidden",
      }}>
        <Box style={{
          position: "absolute", inset: 0,
          backgroundImage:
            "radial-gradient(circle at 20% 50%, rgba(59,130,246,0.15) 0%, transparent 50%)," +
            "radial-gradient(circle at 80% 20%, rgba(99,102,241,0.1) 0%, transparent 40%)",
        }} />

        <Container size="lg" style={{ position: "relative" }}>
          <Stack gap="lg">

            {/* Back link */}
            <Link href="/dashboard/catalog" style={{ textDecoration: "none" }}>
              <Group gap={6} style={{ color: "rgba(255,255,255,0.55)", fontSize: 14 }}>
                <IconArrowLeft size={16} />
                <span>Back to Catalog</span>
              </Group>
            </Link>

            {/* Badges */}
            <Group gap="sm">
              {course.topic && (
                <Badge size="sm" variant="light" color="blue"
                  style={{ textTransform: "uppercase", letterSpacing: "0.08em" }}>
                  {course.topic}
                </Badge>
              )}
              {course.difficulty && (
                <Badge size="sm" variant="light" color={difficultyColor(course.difficulty)}
                  style={{ textTransform: "uppercase", letterSpacing: "0.08em" }}>
                  {difficultyLabel(course.difficulty)}
                </Badge>
              )}
            </Group>

            {/* Title row — no Grid, just flex */}
            <div style={{ display: "flex", alignItems: "flex-start", gap: 24, flexWrap: "wrap" }}>
              <div style={{ flex: "1 1 400px" }}>
                <Title order={1} style={{
                  color: "white",
                  fontSize: "clamp(1.6rem, 3vw, 2.4rem)",
                  lineHeight: 1.2,
                  marginBottom: 12,
                }}>
                  {course.title}
                </Title>
                {previewText && (
                  <Text size="md" style={{ color: "rgba(255,255,255,0.7)", lineHeight: 1.6, maxWidth: 600 }}>
                    {previewText.slice(0, 200)}
                    {previewText.length > 200 ? "..." : ""}
                  </Text>
                )}
              </div>
              <div style={{ flexShrink: 0, paddingTop: 4 }}>
                <CourseEnrollmentButton
                  courseId={course.id}
                  isEnrolled={course.isEnrolled}
                  participantCount={course.participantCount}
                  isInstructor={isInstructor}
                  isPublished={course.isPublished}
                />
              </div>
            </div>

          </Stack>
        </Container>
      </Box>

      {/* ── Main Content ── also no Grid, plain flex */}
      <Container size="lg" py="xl">
        <div style={{ display: "flex", gap: 24, alignItems: "flex-start", flexWrap: "wrap" }}>

          {/* Left column */}
          <div style={{ flex: "1 1 500px", minWidth: 0 }}>
            <Stack gap="lg">

              {/* Course Journey placeholder */}
              <Paper withBorder radius="lg" p="xl" shadow="xs">
                <Stack gap="md">
                  <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                    <Text fw={700} size="lg">Course Journey</Text>
                    <Text fw={700} size="sm" c="blue">33% Complete</Text>
                  </div>
                  <Progress value={33} size="md" radius="xl" color="blue" />
                  <Group gap="xl">
                    <Group gap={6}>
                      <IconCheck size={14} color="var(--mantine-color-green-6)" />
                      <Text size="sm" c="dimmed">4 Lessons Finished</Text>
                    </Group>
                    <Group gap={6}>
                      <IconCircleDashed size={14} color="var(--mantine-color-gray-5)" />
                      <Text size="sm" c="dimmed">8 Lessons Remaining</Text>
                    </Group>
                  </Group>
                </Stack>
              </Paper>

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

          {/* Right column — Instructor card */}
          <div style={{ flex: "0 0 280px", position: "sticky", top: 24 }}>
            {owner && (
              <Paper withBorder radius="lg" p="xl" shadow="xs">
                <Stack gap="md">
                  <Text size="xs" tt="uppercase" fw={700} c="dimmed"
                    style={{ letterSpacing: "0.08em" }}>
                    Instructor
                  </Text>
                  <Divider />
                  <Group gap="md" align="flex-start">
                    <Avatar radius="xl" size="lg" color="blue"
                      src={owner.picture ?? undefined}
                      style={{ border: "2px solid var(--mantine-color-blue-2)" }}>
                      {getInitials(owner.name)}
                    </Avatar>
                    <Stack gap={4} style={{ flex: 1 }}>
                      <Text fw={700} size="sm">{owner.name}</Text>
                      <Group gap={4}>
                        <IconUser size={11} color="var(--mantine-color-dimmed)" />
                        <Text size="xs" c="dimmed">Instructor</Text>
                      </Group>
                    </Stack>
                  </Group>
                </Stack>
              </Paper>
            )}
          </div>

        </div>
      </Container>
    </Box>
  );
}

