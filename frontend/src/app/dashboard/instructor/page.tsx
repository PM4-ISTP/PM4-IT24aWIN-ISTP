import { Alert, Button, Group, Stack, Text, Title } from "@mantine/core";
import { IconPlus } from "@tabler/icons-react";
import Link from "next/link";
import { fetchInstructorCourses } from "@/src/lib/actions/courses";
import { CourseGrid } from "@/src/components/CourseGrid";

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
          <Title order={1} size="h2">
            Manage Courses
          </Title>
          <Text size="sm" c="dimmed">
            Manage or create your courses here.
          </Text>
        </Stack>
        <Link href="/dashboard/instructor/create">
          <Button leftSection={<IconPlus size={16} />}>New course</Button>
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
