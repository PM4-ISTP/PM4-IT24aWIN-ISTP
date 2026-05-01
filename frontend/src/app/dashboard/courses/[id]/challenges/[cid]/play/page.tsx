import { Alert, Container, Group, Stack } from "@mantine/core";
import { IconArrowLeft } from "@tabler/icons-react";
import Link from "next/link";
import { fetchChallengeForPlay } from "@/src/features/course/actions/challenges";
import { ChallengePlayView } from "@/src/features/course/components/play/ChallengePlayView";

export default async function PlayChallengePage({
  params,
}: {
  params: Promise<{ id: string; cid: string }>;
}) {
  const { id: courseId, cid: challengeId } = await params;

  const result = await fetchChallengeForPlay(challengeId, courseId);

  if (!result.success) {
    return (
      <Container>
        <Stack p="xl" gap="lg">
          <Link href={`/dashboard/courses/${courseId}`} style={{ textDecoration: "none" }}>
            <Group gap={6} style={{ color: "rgba(255,255,255,0.45)", fontSize: 14 }}>
              <IconArrowLeft size={16} />
              <span>Back to course</span>
            </Group>
          </Link>
          <Alert color="red" title="Unable to load challenge">
            {result.error}
          </Alert>
        </Stack>
      </Container>
    );
  }

  return (
    <ChallengePlayView
      courseId={courseId}
      challengeId={challengeId}
      initialChallenge={result.data}
    />
  );
}
