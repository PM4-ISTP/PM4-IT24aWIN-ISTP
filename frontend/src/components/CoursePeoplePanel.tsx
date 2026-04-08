"use client";

import { Badge, Divider, Group, Paper, Stack, Text } from "@mantine/core";
import { ROLES } from "@/src/lib/roles";
import type { CourseUserSummary, PlatformRole } from "@/src/types/course";
import UserAvatar from "./UserAvatar";

interface CoursePeoplePanelProps {
  owner: CourseUserSummary | null;
  collaborators: CourseUserSummary[];
}

function getRoleBadge(role: PlatformRole): { label: string; color: string } {
  switch (role) {
    case ROLES.ADMINISTRATOR:
      return { label: "Admin", color: "red" };
    case ROLES.INSTRUCTOR:
      return { label: "Instructor", color: "blue" };
    case ROLES.STUDENT:
      return { label: "Student", color: "gray" };
    default:
      return { label: role, color: "gray" };
  }
}

function PersonCard({ user, courseBadge }: { user: CourseUserSummary; courseBadge?: string }) {
  return (
    <Group align="flex-start" gap="sm" wrap="nowrap">
      <UserAvatar pictureUrl={user.picture} userName={user.name} />
      <Stack gap={4} style={{ flex: 1 }}>
        <Group gap="xs">
          <Text fw={600} size="sm">
            {user.name}
          </Text>
          {courseBadge ? (
            <Badge size="xs" variant="light" color="grape">
              {courseBadge}
            </Badge>
          ) : null}
        </Group>
        <Text size="xs" c="dimmed">
          {user.email}
        </Text>
        <Group gap={6}>
          {user.roles.map((role) => {
            const badge = getRoleBadge(role);
            return (
              <Badge key={role} size="xs" variant="dot" color={badge.color}>
                {badge.label}
              </Badge>
            );
          })}
        </Group>
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
            <PersonCard user={owner} courseBadge="Owner" />
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
                <PersonCard key={user.id} user={user} courseBadge="Collaborator" />
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
