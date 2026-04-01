import { Alert, Button, Group, Paper, Stack, Text, TextInput, Title } from "@mantine/core";
import Link from "next/link";
import { fetchPublishedCourses } from "@/src/lib/actions/courses";
import { CourseGrid } from "@/src/components/CourseGrid";

export default async function CatalogPage(props: {
  searchParams: Promise<{ page?: string; query?: string }>;
}) {
  const searchParams = await props.searchParams;
  const currentPage = Math.max(1, parseInt(searchParams.page ?? "1"));
  const query = searchParams.query ?? "";
  const result = await fetchPublishedCourses(query, currentPage - 1, 12);

  return (
    <Stack p="xl" gap="lg">
      <Stack gap={4}>
        <Title order={1} size="h2">
          Browse / Catalog
        </Title>
        <Text size="sm" c="dimmed">
          Explore all published courses and search by title or description.
        </Text>
      </Stack>

      <Paper withBorder radius="lg" p="lg">
        <form action="/dashboard/catalog" method="get">
          <Group align="flex-end">
            <TextInput
              name="query"
              label="Search courses"
              placeholder="Search by title or description"
              defaultValue={query}
              style={{ flex: 1 }}
            />
            <Group gap="sm">
              <Button type="submit">Search</Button>
              <Link href="/dashboard/catalog">
                <Button variant="light">Reset</Button>
              </Link>
            </Group>
          </Group>
        </form>
      </Paper>

      {result.success ? (
        <CourseGrid
          courses={result.data.content}
          totalPages={result.data.totalPages}
          currentPage={currentPage}
        />
      ) : (
        <Alert color="red" title="Failed to load catalog">
          {result.error}
        </Alert>
      )}
    </Stack>
  );
}
