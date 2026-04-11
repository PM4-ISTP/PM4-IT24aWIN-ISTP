"use client";

import { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import {
  ActionIcon,
  Affix,
  Alert,
  Button,
  Container,
  Group,
  Loader,
  Notification,
  Stack,
  Text,
  Title,
} from "@mantine/core";
import { IconArrowLeft, IconX } from "@tabler/icons-react";
import {
  ChallengeFormFields,
  type ChallengeFormValues,
} from "@/src/components/ChallengeFormFields";
import { fetchChallenge, updateChallenge, deleteChallenge } from "@/src/lib/actions/challenges";
import { normalizeShortDescription } from "@/src/lib/courseText";
import { useToast } from "@/src/hooks/useToast";
import { CHALLENGE_SHORT_DESCRIPTION_MAX_CHARS } from "@/src/lib/challengeConstants";

export default function EditChallenge() {
  const router = useRouter();
  const params = useParams<{ id: string }>();
  const challengeId = params.id;

  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);

  const [formValues, setFormValues] = useState<ChallengeFormValues>({
    title: "",
    shortDescription: "",
    description: "",
    status: "DRAFT",
    difficulty: "MEDIUM",
  });
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);
  const [titleError, setTitleError] = useState<string | null>(null);
  const [shortDescriptionError, setShortDescriptionError] = useState<string | null>(null);
  const [formError, setFormError] = useState<string | null>(null);
  const charLimitToast = useToast();

  useEffect(() => {
    async function load() {
      const result = await fetchChallenge(challengeId);
      if (!result.success) {
        setLoadError(result.error);
        setLoading(false);
        return;
      }

      const challenge = result.data;
      setFormValues({
        title: challenge.title,
        shortDescription: challenge.shortDescription ?? "",
        description: challenge.description ?? "",
        status: challenge.status,
        difficulty: challenge.difficulty,
      });

      setLoading(false);
    }

    void load();
  }, [challengeId]);

  async function handleSubmit() {
    setTitleError(null);
    setShortDescriptionError(null);
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

    setIsSubmitting(true);

    const result = await updateChallenge(challengeId, {
      title: formValues.title.trim(),
      shortDescription: normalizedShortDescription,
      description: formValues.description,
      status: formValues.status,
      difficulty: formValues.difficulty,
    });

    setIsSubmitting(false);

    if (!result.success) {
      setFormError(result.error);
      return;
    }

    router.refresh();
    router.push("/dashboard/instructor/challenges");
  }

  async function handleDelete() {
    const confirmed = window.confirm(
      "Are you sure you want to delete this challenge? This action cannot be undone."
    );
    if (!confirmed) return;

    setIsDeleting(true);

    const result = await deleteChallenge(challengeId);

    setIsDeleting(false);

    if (!result.success) {
      setFormError(result.error);
      return;
    }

    router.refresh();
    router.push("/dashboard/instructor/challenges");
  }

  if (loading) {
    return (
      <Container>
        <Stack p="xl" align="center">
          <Loader />
        </Stack>
      </Container>
    );
  }

  if (loadError) {
    return (
      <Container>
        <Stack p="xl" gap="lg">
          <Group>
            <ActionIcon
              variant="subtle"
              size="lg"
              onClick={() => router.push("/dashboard/instructor/challenges")}
              aria-label="Back to challenges"
            >
              <IconArrowLeft size={20} />
            </ActionIcon>
          </Group>
          <Alert color="red" title="Failed to load challenge">
            {loadError}
          </Alert>
        </Stack>
      </Container>
    );
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
                Edit Challenge
              </Title>
              <Text size="sm" c="dimmed">
                Update the challenge details.
              </Text>
            </Stack>
          </Group>
          <Button
            color="red"
            variant="outline"
            loading={isDeleting}
            disabled={isDeleting || isSubmitting}
            onClick={() => {
              void handleDelete();
            }}
          >
            Delete Challenge
          </Button>
        </Group>

        <Stack gap="lg">
          <ChallengeFormFields
            values={formValues}
            onChange={setFormValues}
            titleError={titleError}
            shortDescriptionError={shortDescriptionError}
            onCharLimitExceeded={() => charLimitToast.show()}
            onShortDescriptionErrorClear={() => setShortDescriptionError(null)}
          />

          {formError && (
            <Alert color="red" title="Failed to update challenge">
              {formError}
            </Alert>
          )}

          <Button
            variant="filled"
            radius="md"
            loading={isSubmitting}
            disabled={isSubmitting || isDeleting}
            onClick={() => {
              void handleSubmit();
            }}
          >
            Save Changes
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
