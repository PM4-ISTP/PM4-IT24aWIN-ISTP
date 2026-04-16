import { Alert, Button, Group, Stack, Text, Title } from "@mantine/core";
import { IconPlus } from "@tabler/icons-react";
import Link from "next/link";
import { fetchInstructorCourses } from "@/src/features/course/actions/courses";
import { CourseGrid } from "@/src/features/course/components/course/CourseGrid";

export default async function InstructorDashboard(props: {
  searchParams: Promise<{ page?: string }>;
}) {
  const searchParams = await props.searchParams;
  const currentPage = Math.max(1, parseInt(searchParams.page ?? "1"));
  const result = await fetchInstructorCourses(currentPage - 1, 12);

  return (
    <Stack p="xl" gap="lg">
      <Group justify="space-between" align="flex-end">
        <Stack gap={4}>
          <Title
            order={1}
            size="h2"
            style={{
              color: "#f1f5f9",
              fontFamily: "var(--font-space-grotesk), sans-serif",
              fontWeight: 700,
            }}
          >
            Manage Courses
          </Title>
          <Text size="sm" style={{ color: "#94a3b8" }}>
            Manage or create your courses here.
          </Text>
        </Stack>
        <Link href="/dashboard/instructor/create">
          <Button
            leftSection={<IconPlus size={16} />}
            radius="md"
            style={{
              background: "linear-gradient(90deg, #2563eb, #4f46e5)",
              border: "none",
              fontFamily: "var(--font-space-grotesk), sans-serif",
              fontWeight: 600,
              boxShadow: "0 2px 12px rgba(79,70,229,0.3)",
            }}
          >
            New course
          </Button>
        </Link>
      </Group>

      {result.success ? (
        <CourseGrid
          courses={result.data.content}
          totalPages={result.data.totalPages}
          currentPage={currentPage}
          coursePathPrefix="/dashboard/instructor"
        />
      ) : (
        <Alert color="red" title="Failed to load courses">
          {result.error}
        </Alert>
      )}
    </Stack>
  );
}
