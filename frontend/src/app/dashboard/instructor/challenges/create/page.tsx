"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import {
  ActionIcon,
  Affix,
  Alert,
  Box,
  Button,
  Container,
  Group,
  Notification,
  Stack,
  Text,
  Title,
} from "@mantine/core";
import { IconArrowLeft, IconX } from "@tabler/icons-react";
import {
  ChallengeFormFields,
  type ChallengeFormValues,
} from "@/src/features/course/components/challenges/ChallengeFormFields";
import { createChallenge } from "@/src/features/course/actions/challenges";
import { normalizeShortDescription } from "@/src/features/course/utils/courseText";
import { toRequestSubTasks, validateSubTasks } from "@/src/features/course/utils/subTasks";
import { useToast } from "@/src/shared/hooks/useToast";
import { useDockerImageCheck } from "@/src/features/course/hooks/useDockerImageCheck";
import {
  CHALLENGE_SHORT_DESCRIPTION_MAX_CHARS,
  DOCKER_IMAGE_ERROR,
  DOCKER_IMAGE_PATTERN,
} from "@/src/features/course/constants/challengeConstants";
import { toUserFriendlyBackendError } from "@/src/shared/lib/userFriendlyBackendError";

export default function CreateChallenge() {
  const router = useRouter();

  const [formValues, setFormValues] = useState<ChallengeFormValues>({
    title: "",
    shortDescription: "",
    description: "<p>Add a description...</p>",
    status: "DRAFT",
    difficulty: "MEDIUM",
    dockerImage: "",
    subTasks: [
      {
        title: "",
        description: "",
        flag: "",
        orderIndex: 0,
      },
    ],
  });
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [titleError, setTitleError] = useState<string | null>(null);
  const [shortDescriptionError, setShortDescriptionError] = useState<string | null>(null);
  const [dockerImageError, setDockerImageError] = useState<string | null>(null);
  const [subTaskErrors, setSubTaskErrors] = useState<
    Array<Partial<Record<"title" | "description" | "flag", string>>>
  >([]);
  const [formError, setFormError] = useState<string | null>(null);
  const charLimitToast = useToast();
  const dockerImageCheck = useDockerImageCheck(formValues.dockerImage);

  async function handleSubmit() {
    setTitleError(null);
    setShortDescriptionError(null);
    setDockerImageError(null);
    setSubTaskErrors([]);
    setFormError(null);

    if (!formValues.title.trim()) {
      setTitleError("Challenge title is required");
      return;
    }

    const normalizedShortDescription = normalizeShortDescription(formValues.shortDescription);
    if (!normalizedShortDescription) {
      setShortDescriptionError("Short description is required");
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
      setDockerImageError(dockerImageCheck.message ?? "Docker image is not reachable");
      return;
    }

    const subTaskValidation = validateSubTasks(formValues.subTasks);
    if (!subTaskValidation.valid) {
      setSubTaskErrors(subTaskValidation.errors);
      if (subTaskValidation.formError) {
        setFormError(subTaskValidation.formError);
      }
      return;
    }

    setIsSubmitting(true);

    const result = await createChallenge({
      title: formValues.title.trim(),
      shortDescription: normalizedShortDescription,
      description: formValues.description,
      status: formValues.status,
      difficulty: formValues.difficulty,
      dockerImage: trimmedDockerImage,
      subTasks: toRequestSubTasks(formValues.subTasks),
    });

    setIsSubmitting(false);

    if (!result.success) {
      setFormError(toUserFriendlyBackendError(result.error) ?? result.error);
      return;
    }

    router.refresh();
    router.push("/dashboard/instructor/challenges");
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
              onClick={() => router.push("/dashboard/instructor/challenges")}
              aria-label="Back to challenges"
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
                Create Challenge
              </Title>
              <Text size="sm" style={{ color: "#94a3b8" }}>
                Fill in the details to create a new challenge.
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
            <ChallengeFormFields
              values={formValues}
              onChange={setFormValues}
              titleError={titleError}
              shortDescriptionError={shortDescriptionError}
              dockerImageError={dockerImageError}
              dockerImageCheckStatus={dockerImageCheck.status}
              dockerImageCheckMessage={dockerImageCheck.message}
              subTaskErrors={subTaskErrors}
              defaultExpandedSubTaskIndex={0}
              onCharLimitExceeded={() => charLimitToast.show()}
              onShortDescriptionErrorClear={() => setShortDescriptionError(null)}
              onDockerImageErrorClear={() => setDockerImageError(null)}
            />

            {formError && (
              <Alert color="red" title="Could not create challenge" variant="light">
                {formError}
              </Alert>
            )}

            <Button
              radius="md"
              loading={isSubmitting}
              disabled={
                isSubmitting ||
                dockerImageCheck.status === "checking" ||
                dockerImageCheck.status === "error"
              }
              onClick={() => {
                void handleSubmit();
              }}
              style={{
                background: "linear-gradient(90deg, #2563eb, #4f46e5)",
                border: "none",
                fontFamily: "var(--font-space-grotesk), sans-serif",
                fontWeight: 600,
                boxShadow: "0 2px 12px rgba(79,70,229,0.3)",
              }}
            >
              Create Challenge
            </Button>
          </Stack>
        </Box>
      </Stack>

      <Affix position={{ bottom: 20, right: 20 }}>
        {charLimitToast.visible && (
          <Notification
            color="orange"
            title="Character limit reached"
            onClose={charLimitToast.hide}
            withCloseButton
            icon={<IconX size={18} />}
          >
            The short description cannot exceed {CHALLENGE_SHORT_DESCRIPTION_MAX_CHARS} characters
            (including spaces).
          </Notification>
        )}
      </Affix>
    </Container>
  );
}
