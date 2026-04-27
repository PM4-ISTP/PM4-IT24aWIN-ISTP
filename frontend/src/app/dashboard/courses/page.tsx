import { CourseGrid } from "@/src/features/course/components/course/CourseGrid";
import { fetchEnrolledCoursesOfLoggedInUser } from "@/src/features/course/actions/courses";
import { Alert, Stack, Title } from "@mantine/core";

export default async function CoursesPage(props: { searchParams: Promise<{ page?: string }> }) {
  const searchParams = await props.searchParams;
  const parsedPage = Number.parseInt(searchParams.page ?? "1", 10);
  const currentPage = Number.isNaN(parsedPage) ? 1 : Math.max(1, parsedPage);
  const pageSize = 20;
  const result = await fetchEnrolledCoursesOfLoggedInUser(currentPage - 1, pageSize);

  return (
    <Stack p="xl" gap="md">
      <Title order={1}>Courses</Title>

      {result.success ? (
        <CourseGrid
          courses={result.data.content ?? []}
          totalPages={result.data.totalPages ?? 1}
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
