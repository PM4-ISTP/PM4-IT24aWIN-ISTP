import {
  Alert,
  Box,
  Button,
  Group,
  NativeSelect,
  Stack,
  Text,
  TextInput,
  Title,
} from "@mantine/core";
import Link from "next/link";
import { fetchCourseTopics, fetchPublishedCourses } from "@/src/features/course/actions/courses";
import { CourseGrid } from "@/src/features/course/components/course/CourseGrid";
import JoinCourseButton from "@/src/features/course/components/enrollment/JoinCourseButton";

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
  const topicData = [
    { value: "", label: "All topics" },
    ...topics.map((t) => ({ value: t, label: t })),
  ];

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

      <Box
        style={{
          background: "rgba(255,255,255,0.04)",
          border: "1px solid rgba(255,255,255,0.08)",
          borderRadius: 14,
          padding: "1.25rem 1.5rem",
          boxShadow: "0 4px 24px rgba(0,0,0,0.25)",
        }}
      >
        <form action="/dashboard/catalog" method="get">
          <Group align="flex-end" wrap="wrap">
            <TextInput
              name="query"
              label="Search courses"
              placeholder="Search by title, short description, or description"
              defaultValue={query}
              style={{ flex: 1 }}
            />
            <NativeSelect
              name="topic"
              label="Topic"
              data={topicData}
              defaultValue={topic}
              w={220}
            />
            <Group gap="sm">
              <JoinCourseButton />
              <Button
                type="submit"
                radius="md"
                style={{
                  background: "linear-gradient(90deg, #2563eb, #4f46e5)",
                  border: "none",
                  fontFamily: "var(--font-space-grotesk), sans-serif",
                  fontWeight: 600,
                  boxShadow: "0 2px 12px rgba(79,70,229,0.3)",
                }}
              >
                Search
              </Button>
              <Link href="/dashboard/catalog">
                <Button
                  variant="outline"
                  radius="md"
                  style={{
                    borderColor: "rgba(255,255,255,0.12)",
                    color: "#e2e8f0",
                    background: "rgba(255,255,255,0.04)",
                    fontFamily: "var(--font-space-grotesk), sans-serif",
                    fontWeight: 600,
                  }}
                >
                  Reset
                </Button>
              </Link>
            </Group>
          </Group>
        </form>
      </Box>

      {result.success ? (
        <CourseGrid
          courses={result.data.content}
          totalPages={result.data.totalPages}
          currentPage={currentPage}
          coursePathPrefix="/dashboard/catalog"
        />
      ) : (
        <Alert color="red" title="Failed to load catalog">
          {result.error}
        </Alert>
      )}
    </Stack>
  );
}
