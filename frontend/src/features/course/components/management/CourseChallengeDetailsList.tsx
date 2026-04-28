import { Badge, Box, Group, Stack, Text, Title } from "@mantine/core";
import {
  ChallengeDetailsCard,
  formatText,
} from "@/src/features/course/components/management/ChallengeDetailsCard";
import {
  getDifficultyColor,
  getStatusColor,
} from "@/src/features/course/constants/challengeConstants";
import PlayChallengeButton from "@/src/features/course/components/challenges/PlayChallengeButton";
import type { ChallengeDetailResponseDto } from "@/src/features/course/actions/challenges";

export function CourseChallengeDetailsList({
  challenges,
  title,
  showIndex,
}: {
  challenges: ChallengeDetailResponseDto[];
  title: string;
  showIndex: boolean;
}) {
  return (
    <>
      {challenges.length === 0 ? (
        <></>
      ) : (
        <Stack gap="md">
          <Group justify="space-between" align="center">
            <Title order={3}>{title}</Title>
            <Text size="sm" c="dimmed">
              {challenges.length} challenges
            </Text>
          </Group>
          <Stack gap="sm">
            {challenges.map((challenge, index) => {
              const challengeTitle = showIndex
                ? "#" + (index + 1) + " " + formatText(challenge.title)
                : formatText(challenge.title);

              return (
                <ChallengeDetailsCard
                  key={challenge.id}
                  challenge={challenge}
                  title={challengeTitle}
                  rightSection={
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
                        <PlayChallengeButton condition={2} />
                      </Box>
                    </Stack>
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
