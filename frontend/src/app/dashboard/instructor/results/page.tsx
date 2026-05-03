import { Alert, Badge, Box, Group, SimpleGrid, Stack, Text, Title } from "@mantine/core";
import { IconChevronRight, IconUsers } from "@tabler/icons-react";
import Link from "next/link";
import { fetchInstructorCourses } from "@/src/features/course/actions/courses";

export default async function InstructorResultsPage(props: {
  searchParams: Promise<{ page?: string }>;
}) {
  const searchParams = await props.searchParams;
  const currentPage = Math.max(1, parseInt(searchParams.page ?? "1"));
  const result = await fetchInstructorCourses(currentPage - 1, 50);

  return (
    <Stack p="xl" gap="lg">
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
          Results Overview
        </Title>
        <Text size="sm" style={{ color: "#94a3b8" }}>
          Select a course to view student performance and submission results.
        </Text>
      </Stack>

      {!result.success ? (
        <Alert color="red" title="Failed to load courses">
          {result.error}
        </Alert>
      ) : result.data.content?.length === 0 ? (
        <Text size="sm" c="dimmed">
          No courses found.
        </Text>
      ) : (
        <SimpleGrid cols={{ base: 1, sm: 2, lg: 3 }} spacing="md">
          {(result.data.content ?? []).map((course) => (
            <Link
              key={course.id}
              href={`/dashboard/instructor/${course.id}/results`}
              style={{ textDecoration: "none" }}
            >
              <Box
                style={{
                  background: "rgba(255,255,255,0.04)",
                  border: "1px solid rgba(255,255,255,0.08)",
                  borderRadius: 14,
                  padding: "1.25rem 1.5rem",
                  cursor: "pointer",
                  transition: "background 0.15s, border-color 0.15s",
                }}
              >
                <Group justify="space-between" align="flex-start">
                  <Stack gap={6} style={{ flex: 1, minWidth: 0 }}>
                    <Text
                      fw={600}
                      size="sm"
                      style={{
                        color: "#f1f5f9",
                        whiteSpace: "nowrap",
                        overflow: "hidden",
                        textOverflow: "ellipsis",
                      }}
                    >
                      {course.title}
                    </Text>

                    <Group gap={6} wrap="wrap">
                      {course.mcAttemptsMode && (
                        <Badge
                          size="xs"
                          variant="light"
                          color={course.mcAttemptsMode === "ONCE" ? "orange" : "blue"}
                        >
                          {course.mcAttemptsMode === "ONCE" ? "Graded" : "Self-learning"}
                        </Badge>
                      )}
                    </Group>

                    <Group gap={4} mt={4}>
                      <IconUsers size={13} color="var(--mantine-color-dimmed)" />
                      <Text size="xs" c="dimmed">
                        View results overview
                      </Text>
                    </Group>
                  </Stack>

                  <IconChevronRight size={16} color="rgba(255,255,255,0.3)" style={{ flexShrink: 0, marginTop: 2 }} />
                </Group>
              </Box>
            </Link>
          ))}
        </SimpleGrid>
      )}
    </Stack>
  );
}
