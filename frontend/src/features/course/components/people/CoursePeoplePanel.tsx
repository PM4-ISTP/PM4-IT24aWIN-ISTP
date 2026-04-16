"use client";

import { useState } from "react";
import {
  Avatar,
  Badge,
  Box,
  Button,
  Divider,
  Group,
  ScrollArea,
  Stack,
  Text,
  TextInput,
} from "@mantine/core";
import { IconSearch, IconUsers } from "@tabler/icons-react";
import { getInitials } from "@/src/shared/lib/utils";
import type { CollaboratorUserResponseDto, CourseParticipantDto } from "@/src/shared/types/course";

const PAGE_SIZE = 5;

interface CoursePeoplePanelProps {
  owner: CollaboratorUserResponseDto | null;
  collaborators: CollaboratorUserResponseDto[];
  participants: CourseParticipantDto[];
}

function PersonCard({ user }: { user: CollaboratorUserResponseDto }) {
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

function ParticipantCard({ participant }: { participant: CourseParticipantDto }) {
  return (
    <Group align="center" gap="sm" wrap="nowrap">
      <Avatar radius="xl" size="sm" color="teal" src={participant.picture ?? undefined}>
        {getInitials(participant.name)}
      </Avatar>
      <Text size="sm" fw={500} style={{ flex: 1, minWidth: 0 }} lineClamp={1}>
        {participant.name}
      </Text>
    </Group>
  );
}

export function CoursePeoplePanel({ owner, collaborators, participants }: CoursePeoplePanelProps) {
  const [search, setSearch] = useState("");
  const [visibleCount, setVisibleCount] = useState(PAGE_SIZE);

  const filtered = participants.filter((p) => p.name.toLowerCase().includes(search.toLowerCase()));
  const visible = filtered.slice(0, visibleCount);
  const hasMore = visibleCount < filtered.length;

  function handleSearchChange(value: string) {
    setSearch(value);
    setVisibleCount(PAGE_SIZE); // reset pagination on new search
  }

  return (
    <Box
      style={{
        background: "var(--ds-card-bg, rgba(255,255,255,0.04))",
        border: "1px solid var(--ds-card-border, rgba(255,255,255,0.08))",
        borderRadius: 14,
        padding: "1.5rem",
        boxShadow: "var(--ds-card-shadow, 0 4px 24px rgba(0,0,0,0.25))",
      }}
    >
      <Stack gap="lg">
        <Stack gap={2}>
          <Text size="xs" tt="uppercase" fw={700} c="dimmed">
            Course Team
          </Text>
          <Text fw={700}>People and access</Text>
          <Text size="sm" c="dimmed">
            Owners and collaborators can manage this course. Participants can join published courses
            and are counted below.
          </Text>
        </Stack>

        {/* Owner */}
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

        {/* Collaborators */}
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

        {/* Participants */}
        <Stack gap="sm">
          <Group justify="space-between">
            <Group gap={6}>
              <IconUsers size={15} />
              <Text fw={600} size="sm">
                Participants
              </Text>
            </Group>
            <Badge size="sm" variant="light" color="teal">
              {participants.length}
            </Badge>
          </Group>

          {participants.length === 0 ? (
            <Text size="sm" c="dimmed">
              No participants have joined yet.
            </Text>
          ) : (
            <Stack gap="xs">
              <TextInput
                placeholder="Search participants…"
                leftSection={<IconSearch size={14} />}
                value={search}
                onChange={(e) => handleSearchChange(e.currentTarget.value)}
                size="xs"
                radius="md"
              />

              {filtered.length === 0 ? (
                <Text size="xs" c="dimmed" ta="center" py={4}>
                  No participants match &quot;{search}&quot;
                </Text>
              ) : (
                <>
                  <ScrollArea.Autosize mah={visible.length >= PAGE_SIZE ? 240 : undefined}>
                    <Stack gap={8} py={4}>
                      {visible.map((p) => (
                        <ParticipantCard key={p.id} participant={p} />
                      ))}
                    </Stack>
                  </ScrollArea.Autosize>

                  {hasMore && (
                    <Button
                      variant="outline"
                      size="xs"
                      radius="md"
                      onClick={() => setVisibleCount((c) => c + PAGE_SIZE)}
                      style={{
                        borderColor: "rgba(255,255,255,0.12)",
                        color: "#94a3b8",
                        background: "rgba(255,255,255,0.04)",
                        fontFamily: "var(--font-space-grotesk), sans-serif",
                      }}
                    >
                      Show more ({filtered.length - visibleCount} remaining)
                    </Button>
                  )}
                </>
              )}
            </Stack>
          )}
        </Stack>
      </Stack>
    </Box>
  );
}
