import { CourseGrid } from "@/src/components/CourseGrid";
import { fetchPublishedCourses } from "@/src/lib/actions/courses";
import { Alert, Stack, Title } from "@mantine/core";

export default async function CoursesPage(props: { searchParams: Promise<{ page?: string }> }) {
  const searchParams = await props.searchParams;
  const currentPage = Math.max(1, parseInt(searchParams.page ?? "1"));
  const result = await fetchPublishedCourses();

  return (
    <Stack p="xl" gap="md">
      <Title order={1}>Courses</Title>

      {result.success ? (
        <CourseGrid
          courses={result.data.content}
          totalPages={result.data.totalPages}
          currentPage={currentPage}
          coursePathPrefix="/dashboard/courses"
        />
      ) : (
        <Alert color="red" title="Failed to load courses">
          {result.error}
        </Alert>
      )}
    </Stack>
  );
}
