import { Group, Progress, Stack, Text, Title } from "@mantine/core";

import {
  ChallengeDetailsCard,
  type ChallengeDetailsCardProps,
  formatText,
} from "@/src/features/course/components/management/ChallengeDetailsCard";

function formatPercent(value: number): string {
  return `${Math.round(value * 100)}%`;
}

interface ChallengeStatisticEntry {
  challenge: ChallengeDetailsCardProps["challenge"];
  solvedRatio: number;
}

const solvedPercentColor = "#2563eb";

export function CourseChallengeStatisticsList({
  statistics,
  title,
}: {
  statistics: ChallengeStatisticEntry[];
  title: string;
}) {
  return (
    <>
      {statistics.length === 0 ? (
        <Text c="dimmed">This course has no challenges.</Text>
      ) : (
        <Stack gap="md">
          <Group justify="space-between" align="center">
            <Title order={3}>{title}</Title>
            <Text size="sm" c="dimmed">
              {statistics.length} challenges
            </Text>
          </Group>

          <Stack gap="sm">
            {statistics.map((entry, index) => {
              const challenge = entry.challenge;
              const titleText = formatText(challenge.title);
              const solvedRation = entry.solvedRatio ?? 0;
              const solvedPercent = formatPercent(solvedRation);

              return (
                <ChallengeDetailsCard
                  key={challenge.id ?? `${index}`}
                  challenge={challenge}
                  title={`${index + 1}. ${titleText}`}
                  actionSection={
                    <>
                      <Group justify="space-between" align="center">
                        <Text size="sm" fw={600}>
                          Solved
                        </Text>
                        <Text size="sm" fw={700} c={solvedPercentColor}>
                          solvedPercent
                        </Text>
                      </Group>
                      <Progress
                        value={solvedRation * 100}
                        color={solvedPercentColor}
                        radius="xl"
                        size="md"
                      />
                      <Text size="xs" c="dimmed" ta="right">
                        {solvedPercent} of participants solved this challenge
                      </Text>
                    </>
                  }
                />
              );
            })}
          </Stack>
        </Stack>
      )}
    </>
  );
}
