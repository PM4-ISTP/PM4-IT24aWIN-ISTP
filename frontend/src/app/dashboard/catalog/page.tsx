import { Stack } from "@mantine/core";
import { fetchCourseTopics, fetchPublishedCourses } from "@/src/features/course/actions/courses";
import { CourseGrid } from "@/src/features/course/components/course/CourseGrid";
import NotifyOnMount from "@/src/shared/components/NotifyOnMount";
import PageHeader from "@/src/shared/components/PageHeader";
import JoinCourseButton from "@/src/features/course/components/enrollment/JoinCourseButton";
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
      <PageHeader
        title="Browse Catalog"
        subtitle="Explore all published courses. Search by title or topic."
        action={<JoinCourseButton size="sm" />}
      />

      <CatalogFilters query={query} topic={topic} topics={topics} />

      {result.success ? (
        <CourseGrid
          courses={result.data.content ?? []}
          totalPages={result.data.totalPages ?? 1}
          currentPage={currentPage}
          coursePathPrefix="/dashboard/catalog"
        />
      ) : (
        <NotifyOnMount
          id="catalog-error"
          color="red"
          title="Could not load catalog"
          message={
            toUserFriendlyBackendError(result.error) ??
            "Something went wrong loading the catalog. Please try again."
          }
        />
      )}
    </Stack>
  );
}
