"use client";

import { Avatar, Badge, Divider, Group, Paper, Stack, Text } from "@mantine/core";
import type { CourseUserSummary } from "@/src/types/course";

interface CoursePeoplePanelProps {
  owner: CourseUserSummary | null;
  collaborators: CourseUserSummary[];
}

function getInitials(name: string): string {
  const parts = name.trim().split(/\s+/).filter(Boolean);
  if (parts.length === 0) {
    return "?";
  }

  return parts
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase() ?? "")
    .join("");
}

function PersonCard({ user }: { user: CourseUserSummary }) {
  return (
    <Group align="flex-start" gap="sm" wrap="nowrap">
      <Avatar radius="xl" color="blue" src={user.picture ?? undefined}>
        {getInitials(user.name)}
      </Avatar>
      <Stack gap={4} style={{ flex: 1 }}>
        <Text fw={600} size="sm">
          {user.name}
        </Text>
        <Text size="xs" c="dimmed">
          {user.email}
        </Text>
      </Stack>
    </Group>
  );
}

export function CoursePeoplePanel({ owner, collaborators }: CoursePeoplePanelProps) {
  return (
    <Paper withBorder radius="lg" p="lg">
      <Stack gap="lg">
        <Stack gap={2}>
          <Text size="xs" tt="uppercase" fw={700} c="dimmed">
            Course Team
          </Text>
          <Text fw={700}>People and access</Text>
          <Text size="sm" c="dimmed">
            Owners and collaborators can manage this course. Participants will appear here once
            enrollment is added.
          </Text>
        </Stack>

        <Stack gap="sm">
          <Text fw={600} size="sm">
            Owner
          </Text>
          {owner ? (
            <PersonCard user={owner} />
          ) : (
            <Text size="sm" c="dimmed">
              No owner found.
            </Text>
          )}
        </Stack>

        <Divider />

        <Stack gap="sm">
          <Group justify="space-between">
            <Text fw={600} size="sm">
              Collaborators
            </Text>
            <Badge size="sm" variant="light" color="blue">
              {collaborators.length}
            </Badge>
          </Group>

          {collaborators.length > 0 ? (
            <Stack gap="md">
              {collaborators.map((user) => (
                <PersonCard key={user.id} user={user} />
              ))}
            </Stack>
          ) : (
            <Text size="sm" c="dimmed">
              No collaborators selected yet.
            </Text>
          )}
        </Stack>

        <Divider />

        <Stack gap="sm">
          <Text fw={600} size="sm">
            Participants
          </Text>
          <Text size="sm" c="dimmed">
            This section is reserved for future enrollments and can include any platform role.
          </Text>
        </Stack>
      </Stack>
    </Paper>
  );
}
