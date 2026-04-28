import type { ReactNode } from "react";

import { Box, Grid, GridCol, Paper, SimpleGrid, Text, Title } from "@mantine/core";

import type { ChallengeDetailResponseDto } from "@/src/features/course/actions/challenges";
import { getSanitizedHtml } from "@/src/shared/lib/utils";

export function formatDateTime(value?: string): string {
  if (!value) return "n/a";

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;

  return date.toLocaleString("de-CH", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

export function formatText(value?: string | number): string {
  if (value === undefined || value === null || value === "") return "n/a";
  return String(value);
}

export interface ChallengeDetailsCardProps {
  challenge: ChallengeDetailResponseDto;
  title: string;
  rightSection: ReactNode;
}

export function ChallengeDetailsCard({
  challenge,
  title,
  rightSection,
}: ChallengeDetailsCardProps) {
  const sanitizedDescription =
    challenge.description === undefined ? "" : getSanitizedHtml(challenge.description);

  return (
    <Paper p="md" radius="md" withBorder style={{ background: "rgba(255,255,255,0.02)" }}>
      <Grid>
        <GridCol span={12}>
          <Title order={4} style={{ fontSize: "1rem" }}>
            {title}
          </Title>
        </GridCol>

        <GridCol span={9}>
          <SimpleGrid cols={{ base: 1, sm: 2 }} spacing="xs">
            <Text size="sm">Short Description: {formatText(challenge.shortDescription)}</Text>
            <Text size="sm">Creator: {formatText(challenge.creator?.name)}</Text>
            <Text size="sm">Created At: {formatDateTime(challenge.createdAt)}</Text>
            <Text size="sm">Updated At: {formatDateTime(challenge.updatedAt)}</Text>
            <Text size="sm">Max Score: {formatText(challenge.maxScore)}</Text>
          </SimpleGrid>

          <Box>
            <Text size="sm" fw={600} mt={30} mb={4}>
              Description:
            </Text>
            <Box
              className="course-description"
              style={{ fontSize: "var(--mantine-font-size-sm)" }}
              dangerouslySetInnerHTML={{ __html: sanitizedDescription }}
            />
          </Box>
        </GridCol>

        <GridCol span={3}>{rightSection}</GridCol>
      </Grid>
    </Paper>
  );
}
