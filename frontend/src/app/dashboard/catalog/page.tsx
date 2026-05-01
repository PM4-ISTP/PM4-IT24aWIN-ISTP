import { Stack, Text, Title } from "@mantine/core";
import { fetchCourseTopics, fetchPublishedCourses } from "@/src/features/course/actions/courses";
import { CourseGrid } from "@/src/features/course/components/course/CourseGrid";
import ToastOnMount from "@/src/shared/components/ToastOnMount";
import { toUserFriendlyBackendError } from "@/src/shared/lib/userFriendlyBackendError";
import CatalogFilters from "@/src/app/dashboard/catalog/CatalogFilters";

export default async function CatalogPage(props: {
  searchParams: Promise<{ page?: string; query?: string; topic?: string }>;
}) {
  const searchParams = await props.searchParams;
  const currentPage = Math.max(1, parseInt(searchParams.page ?? "1"));
  const query = searchParams.query ?? "";
  const topic = searchParams.topic ?? "";

  const [result, topicsResult] = await Promise.all([
    fetchPublishedCourses(query, currentPage - 1, 12, topic),
    fetchCourseTopics(),
  ]);

  const topics = topicsResult.success ? topicsResult.data : [];

  return (
    <Stack p="xl" gap="lg">
      <Stack gap={4}>
        <Title order={1} size="h2" style={{ color: "#f1f5f9" }}>
          Browse / Catalog
        </Title>
        <Text size="sm" style={{ color: "#94a3b8" }}>
          Explore all published courses and search by title, short description, or description.
        </Text>
      </Stack>

      <CatalogFilters query={query} topic={topic} topics={topics} />

      {result.success ? (
        <CourseGrid
          courses={result.data.content ?? []}
          totalPages={result.data.totalPages ?? 1}
          currentPage={currentPage}
          coursePathPrefix="/dashboard/catalog"
        />
      ) : (
        <ToastOnMount
          color="red"
          title="Failed to load catalog"
          message={toUserFriendlyBackendError(result.error) ?? "Please try again."}
        />
      )}
    </Stack>
  );
}
