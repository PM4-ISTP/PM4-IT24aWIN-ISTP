import { Alert, Stack } from "@mantine/core";
import { IconPlus } from "@tabler/icons-react";
import { fetchInstructorCourses } from "@/src/features/course/actions/courses";
import { CourseGrid } from "@/src/features/course/components/course/CourseGrid";
import PageHeader from "@/src/shared/components/PageHeader";
import AppButton from "@/src/shared/components/AppButton";

export default async function InstructorDashboard(props: {
  searchParams: Promise<{ page?: string }>;
}) {
  const searchParams = await props.searchParams;
  const currentPage = Math.max(1, parseInt(searchParams.page ?? "1"));
  const result = await fetchInstructorCourses(currentPage - 1, 12);

  return (
    <Stack p="xl" gap="lg">
      <PageHeader
        title="Manage Courses"
        subtitle="Manage or create your courses here."
        action={
          <AppButton
            component="a"
            href="/dashboard/instructor/create"
            leftSection={<IconPlus size={16} />}
          >
            New course
          </AppButton>
        }
      />

      {result.success ? (
        <CourseGrid
          courses={result.data.content ?? []}
          totalPages={result.data.totalPages ?? 1}
          currentPage={currentPage}
          coursePathPrefix="/dashboard/instructor"
          enableRemove
        />
      ) : (
        <Alert color="red" title="Failed to load courses">
          {result.error}
        </Alert>
      )}
    </Stack>
  );
}
