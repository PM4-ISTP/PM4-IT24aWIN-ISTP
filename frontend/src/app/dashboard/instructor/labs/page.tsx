import { Alert, Stack } from "@mantine/core";
import { IconPlus } from "@tabler/icons-react";
import { fetchInstructorLabs } from "@/src/features/course/actions/labs";
import { LabGrid } from "@/src/features/course/components/labs/LabGrid";
import PageHeader from "@/src/shared/components/PageHeader";
import AppButton from "@/src/shared/components/AppButton";

export default async function InstructorLabs(props: { searchParams: Promise<{ page?: string }> }) {
  const searchParams = await props.searchParams;
  const currentPage = Math.max(1, parseInt(searchParams.page ?? "1"));
  const result = await fetchInstructorLabs(currentPage - 1, 12);

  return (
    <Stack p="xl" gap="lg">
      <PageHeader
        title="Labs"
        subtitle="Manage or create your reusable labs here."
        action={
          <AppButton
            component="a"
            href="/dashboard/instructor/labs/create"
            leftSection={<IconPlus size={16} />}
          >
            New Lab
          </AppButton>
        }
      />

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
