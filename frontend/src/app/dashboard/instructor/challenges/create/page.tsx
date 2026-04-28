"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import {
  ActionIcon,
  Affix,
  Alert,
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
import {
  CHALLENGE_SHORT_DESCRIPTION_MAX_CHARS,
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
      setDockerImageError(
        "Docker image must be a valid image reference (e.g. image, registry/image, registry/image:tag)"
      );
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
              onClick={() => router.push("/dashboard/instructor/challenges")}
              aria-label="Back to challenges"
            >
              <IconArrowLeft size={20} />
            </ActionIcon>
            <Stack gap={4}>
              <Title order={1} size="h2">
                Create Challenge
              </Title>
              <Text size="sm" c="dimmed">
                Fill in the details to create a new challenge.
              </Text>
            </Stack>
          </Group>
        </Group>

        <Stack gap="lg">
          <ChallengeFormFields
            values={formValues}
            onChange={setFormValues}
            titleError={titleError}
            shortDescriptionError={shortDescriptionError}
            dockerImageError={dockerImageError}
            subTaskErrors={subTaskErrors}
            defaultExpandedSubTaskIndex={0}
            onCharLimitExceeded={() => charLimitToast.show()}
            onShortDescriptionErrorClear={() => setShortDescriptionError(null)}
            onDockerImageErrorClear={() => setDockerImageError(null)}
          />

          {formError && (
            <Alert color="red" title="Failed to create challenge">
              {formError}
            </Alert>
          )}

          <Button
            variant="filled"
            radius="md"
            loading={isSubmitting}
            disabled={isSubmitting}
            onClick={() => {
              void handleSubmit();
            }}
          >
            Create Challenge
          </Button>
        </Stack>
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
