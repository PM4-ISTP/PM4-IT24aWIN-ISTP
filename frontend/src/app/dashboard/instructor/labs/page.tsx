import { Alert, Button, Group, Stack, Text, Title } from "@mantine/core";
import { IconPlus } from "@tabler/icons-react";
import Link from "next/link";
import { fetchInstructorLabs } from "@/src/features/course/actions/labs";
import { LabGrid } from "@/src/features/course/components/labs/LabGrid";

export default async function InstructorLabs(props: {
  searchParams: Promise<{ page?: string }>;
}) {
  const searchParams = await props.searchParams;
  const currentPage = Math.max(1, parseInt(searchParams.page ?? "1"));
  const result = await fetchInstructorLabs(currentPage - 1, 12);

  return (
    <Stack p="xl" gap="lg">
      <Group justify="space-between" align="flex-end" wrap="wrap" gap="sm">
        <div>
          <Title
            order={1}
            size="h2"
            style={{
              color: "#f1f5f9",
              fontFamily: "var(--font-space-grotesk), sans-serif",
              fontWeight: 700,
            }}
          >
            Labs
          </Title>
          <Text size="sm" style={{ color: "#94a3b8" }} mt={4}>
            Manage or create your reusable labs here.
          </Text>
        </div>
        <Link href="/dashboard/instructor/labs/create">
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
            New Lab
          </Button>
        </Link>
      </Group>

      {result.success ? (
        <LabGrid
          labs={(result.data.content ?? []).map((c) => ({
            id: c.id ?? "",
            title: c.title ?? "",
            status: c.status ?? "DRAFT",
            difficulty: c.difficulty ?? "MEDIUM",
            maxScore: c.maxScore ?? 0,
            courseCount: c.courseCount ?? 0,
            updatedAt: c.updatedAt ?? "",
          }))}
          totalPages={result.data.totalPages ?? 0}
          currentPage={currentPage}
        />
      ) : (
        <Alert color="red" title="Could not load labs" variant="light">
          Something went wrong loading your labs. Please refresh the page.
        </Alert>
      )}
    </Stack>
  );
}
