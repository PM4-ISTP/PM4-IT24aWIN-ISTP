import { Badge, Box, Group, Stack, Text, UnstyledButton } from "@mantine/core";
import { IconBook, IconClock } from "@tabler/icons-react";
import classes from "./ChallengeCard.module.css";
import { getDifficultyColor, getStatusColor } from "@/src/lib/challengeConstants";

export interface ChallengeCardProps {
  id: string;
  title: string;
  status: string; // "DRAFT" | "PRIVATE" | "PUBLIC"
  difficulty: string; // "BEGINNER" | "EASY" | "MEDIUM" | "HARD" | "EXPERT"
  maxScore: number;
  creatorName: string;
  courseCount: number;
  updatedAt: string;
  onClick: (id: string) => void;
}

export function ChallengeCard({
  id,
  title,
  status,
  difficulty,
  courseCount,
  updatedAt,
  onClick,
}: ChallengeCardProps) {
  return (
    <UnstyledButton className={classes.card} onClick={() => onClick(id)}>
      <Stack gap="sm" h="100%">
        <Text fw={500} size="sm" lineClamp={2}>
          {title}
        </Text>

        <Group gap="xs">
          <Badge size="xs" variant="light" color={getStatusColor(status)}>
            {status}
          </Badge>
          <Badge size="xs" variant="light" color={getDifficultyColor(difficulty)}>
            {difficulty}
          </Badge>
        </Group>

        <Box className={classes.footer}>
          <Group gap="xs">
            <IconBook size={13} stroke={1.5} />
            <Text size="xs" c="dimmed">
              {courseCount} course{courseCount !== 1 ? "s" : ""}
            </Text>
          </Group>
          <Group gap="xs">
            <IconClock size={13} stroke={1.5} />
            <Text size="xs" c="dimmed">
              {updatedAt}
            </Text>
          </Group>
        </Box>
      </Stack>
    </UnstyledButton>
  );
}
