"use client";

import Link from "next/link";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { useState } from "react";
import {
  ActionIcon,
  Badge,
  Box,
  Button,
  Collapse,
  Group,
  Pagination,
  Stack,
  Text,
  TextInput,
} from "@mantine/core";
import { IconChevronDown, IconChevronRight } from "@tabler/icons-react";

type ChallengeProgressUser = {
  id?: string | null;
  name?: string | null;
  username?: string | null;
  email?: string | null;
};

type ChallengeProgressSubTask = {
  isCompleted: boolean;
  subTask: {
    id: string;
    orderIndex: number;
    title: string;
  };
};

type ChallengeProgress = {
  user: ChallengeProgressUser;
  subTasks: ChallengeProgressSubTask[];
};

type ChallengeProgressListProps = {
  progresses: ChallengeProgress[];
  query: string;
  currentPage: number;
  totalPages: number;
};

function buildUserLabel(user: ChallengeProgress["user"]) {
  const fullName = user.name?.trim() || "Unnamed participant";
  return `${fullName} (${user.username}, ${user.email})`;
}

function buildSolvedLabel(progress: ChallengeProgress) {
  const solvedCount = progress.subTasks.filter((subTask) => subTask.isCompleted).length;
  return `${solvedCount} / ${progress.subTasks.length} subtasks solved`;
}

export function ChallengeProgressList({
  progresses,
  query,
  currentPage,
  totalPages,
}: ChallengeProgressListProps) {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const [expandedUserId, setExpandedUserId] = useState<string | null>(null);

  function buildPageUrl(nextPage: number) {
    const params = new URLSearchParams(searchParams.toString());

    if (query.trim()) params.set("query", query.trim());
    else params.delete("query");

    if (nextPage > 1) params.set("page", nextPage.toString());
    else params.delete("page");

    const nextQuery = params.toString();
    return nextQuery ? `${pathname}?${nextQuery}` : pathname;
  }

  function handlePageChange(nextPage: number) {
    router.push(buildPageUrl(nextPage));
  }

  return (
    <Stack gap="lg">
      <Box
        style={{
          background: "rgba(255,255,255,0.04)",
          border: "1px solid rgba(255,255,255,0.08)",
          borderRadius: 14,
          padding: "1.25rem 1.5rem",
          boxShadow: "0 4px 24px rgba(0,0,0,0.25)",
        }}
      >
        <form action={pathname} method="get">
          <Group align="flex-end" wrap="wrap">
            <TextInput
              name="query"
              label="Search participants"
              placeholder="Search by name, username, or email"
              defaultValue={query}
              style={{ flex: 1, minWidth: 260 }}
            />

            <Group gap="sm">
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
              <Link href={pathname}>
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

      {progresses.length === 0 ? (
        <Text c="dimmed">No participant progress found.</Text>
      ) : (
        <Stack gap="md">
          {progresses.map((progress) => {
            const userId = progress.user.id as string; //
            const isExpanded = expandedUserId === userId;
            const solvedLabel = buildSolvedLabel(progress);

            return (
              <Box
                key={progress.user.id}
                style={{
                  border: "1px solid rgba(255,255,255,0.08)",
                  borderRadius: 14,
                  overflow: "hidden",
                  background: "rgba(255,255,255,0.03)",
                }}
              >
                <Box
                  p="md"
                  style={{ cursor: "pointer" }}
                  onClick={() => setExpandedUserId(isExpanded ? null : userId)}
                >
                  <Group justify="space-between" align="center" wrap="nowrap">
                    <Group gap="sm" wrap="nowrap" align="center" style={{ flex: 1, minWidth: 0 }}>
                      <ActionIcon variant="transparent" size="sm" tabIndex={-1}>
                        {isExpanded ? (
                          <IconChevronDown size={16} />
                        ) : (
                          <IconChevronRight size={16} />
                        )}
                      </ActionIcon>

                      <Stack gap={2} style={{ minWidth: 0 }}>
                        <Text fw={600} style={{ color: "#e2e8f0" }} lineClamp={1}>
                          {buildUserLabel(progress.user)}
                        </Text>
                        <Text size="sm" c="dimmed">
                          {solvedLabel}
                        </Text>
                      </Stack>
                    </Group>

                    <Badge variant="light" color="blue" radius="sm">
                      {progress.subTasks.filter((subTask) => subTask.isCompleted).length} /{" "}
                      {progress.subTasks.length}
                    </Badge>
                  </Group>
                </Box>

                <Collapse expanded={isExpanded}>
                  <Box
                    p="md"
                    style={{
                      borderTop: "1px solid rgba(255,255,255,0.08)",
                      background: "rgba(255,255,255,0.02)",
                    }}
                  >
                    <Stack gap="sm">
                      {progress.subTasks.map((subTask) => (
                        <Group
                          key={subTask.subTask.id}
                          justify="space-between"
                          align="center"
                          wrap="nowrap"
                          p="sm"
                          style={{
                            border: "1px solid rgba(255,255,255,0.06)",
                            borderRadius: 12,
                            background: "rgba(255,255,255,0.03)",
                          }}
                        >
                          <Stack gap={2} style={{ minWidth: 0 }}>
                            <Text fw={600} style={{ color: "#e2e8f0" }} lineClamp={1}>
                              {subTask.subTask.orderIndex}. {subTask.subTask.title}
                            </Text>
                          </Stack>

                          <Badge variant="light" color={subTask.isCompleted ? "green" : "gray"}>
                            {subTask.isCompleted ? "Solved" : "Not solved"}
                          </Badge>
                        </Group>
                      ))}
                    </Stack>
                  </Box>
                </Collapse>
              </Box>
            );
          })}
        </Stack>
      )}

      {totalPages > 1 && (
        <Group justify="center">
          <Pagination
            total={totalPages}
            value={currentPage}
            onChange={handlePageChange}
            size="sm"
          />
        </Group>
      )}
    </Stack>
  );
}
