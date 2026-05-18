"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { ActionIcon, Alert, Box, Container, Group, Stack, Text, Title } from "@mantine/core";
import { IconArrowLeft } from "@tabler/icons-react";
import AppButton from "@/src/shared/components/AppButton";
import {
  LabFormFields,
  type ChallengeFormValues,
} from "@/src/features/course/components/labs/LabFormFields";
import { createLab } from "@/src/features/course/actions/labs";
import { toRequestChallenges, validateChallenges } from "@/src/features/course/utils/challenges";
import { useDockerImageCheck } from "@/src/features/course/hooks/useDockerImageCheck";
import {
  DOCKER_IMAGE_ERROR,
  DOCKER_IMAGE_PATTERN,
} from "@/src/features/course/constants/challengeConstants";
import { toUserFriendlyBackendError } from "@/src/shared/lib/userFriendlyBackendError";

export default function CreateLab() {
  const router = useRouter();

  const [formValues, setFormValues] = useState<ChallengeFormValues>({
    title: "",
    description: "<p>Add a description...</p>",
    status: "DRAFT",
    difficulty: "MEDIUM",
    dockerImage: "",
    containerPort: 80,
    podTtlSeconds: undefined,
    challenges: [
      {
        title: "",
        description: "",
        flag: "",
        orderIndex: 0,
        type: "FLAG",
        points: 1,
        hint: "",
        options: [
          { text: "", isCorrect: true, orderIndex: 0 },
          { text: "", isCorrect: false, orderIndex: 1 },
        ],
      },
    ],
  });
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [titleError, setTitleError] = useState<string | null>(null);
  const [dockerImageError, setDockerImageError] = useState<string | null>(null);
  const [challengeErrors, setChallengeErrors] = useState<
    Array<Partial<Record<"title" | "description" | "flag" | "options", string>>>
  >([]);
  const [formError, setFormError] = useState<string | null>(null);
  const dockerImageCheck = useDockerImageCheck(formValues.dockerImage);

  async function handleSubmit() {
    setTitleError(null);
    setDockerImageError(null);
    setChallengeErrors([]);
    setFormError(null);

    if (!formValues.title.trim()) {
      setTitleError("Lab title is required");
      return;
    }

    const trimmedDockerImage = formValues.dockerImage.trim();
    if (!trimmedDockerImage) {
      setDockerImageError("Docker image is required");
      return;
    }
    if (!DOCKER_IMAGE_PATTERN.test(trimmedDockerImage)) {
      setDockerImageError(DOCKER_IMAGE_ERROR);
      return;
    }
    if (dockerImageCheck.status === "checking") {
      setDockerImageError("Please wait until the Docker image check finishes");
      return;
    }
    if (dockerImageCheck.status === "error") {
      setDockerImageError(dockerImageCheck.message ?? "Public GHCR image is not reachable");
      return;
    }

    const challengeValidation = validateChallenges(formValues.challenges);
    if (!challengeValidation.valid) {
      setChallengeErrors(challengeValidation.errors);
      if (challengeValidation.formError) {
        setFormError(challengeValidation.formError);
      }
      return;
    }

    setIsSubmitting(true);

    const result = await createLab({
      title: formValues.title.trim(),
      description: formValues.description,
      status: formValues.status,
      difficulty: formValues.difficulty,
      dockerImage: trimmedDockerImage,
      containerPort: formValues.containerPort,
      ...(formValues.podTtlSeconds !== undefined
        ? { podTtlSeconds: formValues.podTtlSeconds }
        : {}),
      challenges: toRequestChallenges(formValues.challenges),
    });

    setIsSubmitting(false);

    if (!result.success) {
      setFormError(toUserFriendlyBackendError(result.error) ?? result.error);
      return;
    }

    router.refresh();
    router.push("/dashboard/instructor/labs");
  }

  return (
    <Container>
      <Stack p="xl" gap="lg">
        <Group justify="space-between" align="flex-end">
          <Group gap="md" align="center">
            <ActionIcon
              variant="subtle"
              size="lg"
              radius="md"
              onClick={() => router.push("/dashboard/instructor/labs")}
              aria-label="Back to labs"
            >
              <IconArrowLeft size={20} />
            </ActionIcon>
            <Stack gap={4}>
              <Title
                order={1}
                size="h2"
                style={{
                  color: "#f1f5f9",
                  fontFamily: "var(--font-space-grotesk), sans-serif",
                  fontWeight: 700,
                }}
              >
                Create Lab
              </Title>
              <Text size="sm" style={{ color: "#94a3b8" }}>
                Fill in the details to create a new lab.
              </Text>
            </Stack>
          </Group>
        </Group>

        <Box
          style={{
            background: "rgba(255,255,255,0.04)",
            border: "1px solid rgba(255,255,255,0.08)",
            borderRadius: 14,
            padding: "2rem",
            boxShadow: "0 4px 24px rgba(0,0,0,0.25)",
          }}
        >
          <Stack gap="lg">
            <LabFormFields
              values={formValues}
              onChange={setFormValues}
              titleError={titleError}
              dockerImageError={dockerImageError}
              dockerImageCheckStatus={dockerImageCheck.status}
              dockerImageCheckMessage={dockerImageCheck.message}
              challengeErrors={challengeErrors}
              defaultExpandedChallengeIndex={0}
              onDockerImageErrorClear={() => setDockerImageError(null)}
            />

            {formError && (
              <Alert color="red" title="Could not create lab" variant="light">
                {formError}
              </Alert>
            )}

            <AppButton
              loading={isSubmitting}
              disabled={
                isSubmitting ||
                dockerImageCheck.status === "checking" ||
                dockerImageCheck.status === "error"
              }
              onClick={() => {
                void handleSubmit();
              }}
            >
              Create Lab
            </AppButton>
          </Stack>
        </Box>
      </Stack>
    </Container>
  );
}
