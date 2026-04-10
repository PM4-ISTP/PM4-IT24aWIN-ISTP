import { Avatar, Divider, Group, Paper, Stack, Text } from "@mantine/core";
import { IconUser } from "@tabler/icons-react";
import { getInitials } from "@/src/lib/utils";
import type { CourseUserSummary } from "@/src/types/course";

interface CourseInstructorCardProps {
  instructor: CourseUserSummary;
}

export function CourseInstructorCard({ instructor }: CourseInstructorCardProps) {
  return (
    <Paper withBorder radius="lg" p="xl" shadow="xs">
      <Stack gap="md">
        <Text size="xs" tt="uppercase" fw={700} c="dimmed" style={{ letterSpacing: "0.08em" }}>
          Instructor
        </Text>
        <Divider />
        <Group gap="md" align="flex-start">
          <Avatar
            radius="xl"
            size="lg"
            color="blue"
            src={instructor.picture ?? undefined}
            style={{ border: "2px solid var(--mantine-color-blue-2)" }}
          >
            {getInitials(instructor.name)}
          </Avatar>
          <Stack gap={4} style={{ flex: 1 }}>
            <Text fw={700} size="sm">
              {instructor.name}
            </Text>
            <Group gap={4}>
              <IconUser size={11} color="var(--mantine-color-dimmed)" />
              <Text size="xs" c="dimmed">
                Instructor
              </Text>
            </Group>
          </Stack>
        </Group>
      </Stack>
    </Paper>
  );
}
