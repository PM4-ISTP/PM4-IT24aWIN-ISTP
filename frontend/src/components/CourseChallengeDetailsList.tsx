import {
  Badge,
  Box,
  Grid,
  GridCol,
  Group,
  Paper,
  SimpleGrid,
  Stack,
  Text,
  Title,
} from "@mantine/core";
import type { components } from "@/src/lib/api/schema";
import { getDifficultyColor, getStatusColor } from "@/src/lib/challengeConstants";
import PlayChallengeButton from "@/src/components/PlayChallengeButton";
import { getSanitizedHtml } from "@/src/lib/utils";

type ChallengeDetailResponseDto = components["schemas"]["ChallengeDetailResponseDto"];

export type LoadedChallenge = {
  challenge?: ChallengeDetailResponseDto;
  errorMessage?: string;
  loadWasSuccessful: boolean;
};

type CourseChallengeDetailsListProps = {
  loadedChallenges: LoadedChallenge[];
};

function formatDateTime(value?: string): string {
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

function formatText(value?: string | number): string {
  if (value === undefined || value === null || value === "") return "n/a";
  return String(value);
}

let idWhenLoadNotSuccessful = 0;

export function CourseChallengeDetailsList({ loadedChallenges }: CourseChallengeDetailsListProps) {
  return (
    <Stack gap="md">
      <Group justify="space-between" align="center">
        <Title order={3}>Course Challenges</Title>
        <Text size="sm" c="dimmed">
          {loadedChallenges.length} challenges
        </Text>
      </Group>

      {loadedChallenges.length === 0 ? (
        <Text size="sm" c="dimmed">
          No challenges assigned to this course.
        </Text>
      ) : (
        <Stack gap="sm">
          {loadedChallenges.map((loadedChallenge, index) => {
            if (!loadedChallenge.loadWasSuccessful) {
              return (
                <Paper
                  key={idWhenLoadNotSuccessful++}
                  p="md"
                  radius="md"
                  withBorder
                  style={{ background: "rgba(255,255,255,0.02)" }}
                >
                  <Text mb={30}>Could not load this challenge.</Text>
                  <Text>Reason: {loadedChallenge.errorMessage}</Text>
                </Paper>
              );
            }

            const challenge = loadedChallenge.challenge!;
            const sanitizedDescription =
              challenge.description === undefined ? "" : getSanitizedHtml(challenge.description);

            return (
              <Paper
                key={challenge.id}
                p="md"
                radius="md"
                withBorder
                style={{ background: "rgba(255,255,255,0.02)" }}
              >
                <Grid>
                  <GridCol span={12}>
                    <Title order={4} style={{ fontSize: "1rem" }}>
                      #{index + 1} {formatText(challenge.title)}
                    </Title>
                  </GridCol>

                  <GridCol span={9}>
                    <SimpleGrid cols={{ base: 1, sm: 2 }} spacing="xs">
                      <Text size="sm">
                        Short Description: {formatText(challenge.shortDescription)}
                      </Text>
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

                  <GridCol span={3}>
                    <Stack gap="xs" align="flex-end">
                      <Group gap="xs">
                        <Badge variant="light" color={getStatusColor(challenge.status ?? "")}>
                          {formatText(challenge.status)}
                        </Badge>
                        <Badge
                          variant="light"
                          color={getDifficultyColor(challenge.difficulty ?? "")}
                        >
                          {formatText(challenge.difficulty)}
                        </Badge>
                      </Group>

                      <Box my={30} style={{ width: 200 }}>
                        <PlayChallengeButton condition={1} />
                      </Box>
                    </Stack>
                  </GridCol>
                </Grid>
              </Paper>
            );
          })}
        </Stack>
      )}
    </Stack>
  );
}
