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
  Modal,
  Notification,
  Stack,
  Text,
  Title,
} from "@mantine/core";
import { useDisclosure } from "@mantine/hooks";
import { IconArrowLeft, IconTrash, IconX } from "@tabler/icons-react";
import {
  ChallengeFormFields,
  type ChallengeFormValues,
} from "@/src/features/course/components/challenges/ChallengeFormFields";
import {
  fetchChallenge,
  updateChallenge,
  deleteChallenge,
  previewVisibilityImpact,
  type ChallengeStatusEnum,
} from "@/src/features/course/actions/challenges";
import { normalizeShortDescription } from "@/src/features/course/utils/courseText";
import {
  toFormSubTasks,
  toRequestSubTasks,
  validateSubTasks,
} from "@/src/features/course/utils/subTasks";
import { useToast } from "@/src/shared/hooks/useToast";
import {
  CHALLENGE_SHORT_DESCRIPTION_MAX_CHARS,
  DOCKER_IMAGE_PATTERN,
} from "@/src/features/course/constants/challengeConstants";

function isMoreRestrictive(
  oldStatus: ChallengeStatusEnum,
  newStatus: ChallengeStatusEnum
): boolean {
  if (oldStatus === newStatus) return false;
  if (newStatus === "DRAFT") return true;
  if (newStatus === "PRIVATE" && oldStatus === "PUBLIC") return true;
  return false;
}

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
    dockerImage: "",
    subTasks: [],
  });
  const [initialStatus, setInitialStatus] = useState<ChallengeStatusEnum>("DRAFT");
  const [courseCount, setCourseCount] = useState(0);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState<string | null>(null);
  const [deleteOpened, { open: openDelete, close: closeDelete }] = useDisclosure(false);
  const [visibilityOpened, { open: openVisibility, close: closeVisibility }] = useDisclosure(false);
  const [visibilityImpactCount, setVisibilityImpactCount] = useState(0);
  const [titleError, setTitleError] = useState<string | null>(null);
  const [shortDescriptionError, setShortDescriptionError] = useState<string | null>(null);
  const [dockerImageError, setDockerImageError] = useState<string | null>(null);
  const [subTaskErrors, setSubTaskErrors] = useState<
    Array<Partial<Record<"title" | "description" | "flag", string>>>
  >([]);
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
      const loadedStatus = challenge.status ?? "DRAFT";
      setFormValues({
        title: challenge.title ?? "",
        shortDescription: challenge.shortDescription ?? "",
        description: challenge.description ?? "",
        status: loadedStatus,
        difficulty: challenge.difficulty ?? "MEDIUM",
        dockerImage: challenge.dockerImage ?? "",
        dockerImage: challenge.dockerImage ?? "",
        subTasks: toFormSubTasks(challenge.subTasks),
      });
      setInitialStatus(loadedStatus);
      setCourseCount(challenge.courseCount ?? 0);

      setLoading(false);
    }

    void load();
  }, [challengeId]);

  async function performUpdate() {
    const normalizedShortDescription = normalizeShortDescription(formValues.shortDescription);
    if (!normalizedShortDescription) {
      setShortDescriptionError("Short description is required");
      return;
    }

    const trimmedDockerImage = formValues.dockerImage.trim();
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

    const result = await updateChallenge(challengeId, {
      title: formValues.title.trim(),
      shortDescription: normalizedShortDescription,
      description: formValues.description,
      status: formValues.status,
      difficulty: formValues.difficulty,
      dockerImage: trimmedDockerImage,
      dockerImage: trimmedDockerImage,
      subTasks: toRequestSubTasks(formValues.subTasks),
    });

    setIsSubmitting(false);

    if (!result.success) {
      setFormError(result.error);
      return;
    }

    router.refresh();
    router.push("/dashboard/instructor/challenges");
  }

  async function handleSubmit() {
    setTitleError(null);
    setShortDescriptionError(null);
    setDockerImageError(null);
    setDockerImageError(null);
    setSubTaskErrors([]);
    setFormError(null);

    if (!formValues.title.trim()) {
      setTitleError("Challenge title is required");
      return;
    }

    if (!normalizeShortDescription(formValues.shortDescription)) {
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

    if (isMoreRestrictive(initialStatus, formValues.status)) {
      setIsSubmitting(true);
      const preview = await previewVisibilityImpact(challengeId, formValues.status);
      setIsSubmitting(false);

      if (!preview.success) {
        setFormError(preview.error);
        return;
      }

      const count = preview.data.affectedCourseCount ?? 0;
      if (count > 0) {
        setVisibilityImpactCount(count);
        openVisibility();
        return;
      }
    }

    await performUpdate();
  }

  async function handleVisibilityConfirm() {
    closeVisibility();
    await performUpdate();
  }

  async function handleDelete() {
    setIsDeleting(true);
    setDeleteError(null);

    const result = await deleteChallenge(challengeId);

    setIsDeleting(false);

    if (!result.success) {
      setDeleteError(result.error);
      return;
    }

    closeDelete();
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
      <Modal
        opened={visibilityOpened}
        onClose={closeVisibility}
        title="Confirm visibility change"
        centered
      >
        <Stack gap="md">
          <Text size="sm">
            Lowering the visibility of <strong>{formValues.title}</strong> will remove it from{" "}
            {visibilityImpactCount} course{visibilityImpactCount !== 1 ? "s" : ""}.
          </Text>
          <Text size="sm" c="dimmed">
            This action cannot be undone.
          </Text>
          <Group justify="flex-end" gap="sm">
            <Button variant="default" onClick={closeVisibility} disabled={isSubmitting}>
              Cancel
            </Button>
            <Button
              color="orange"
              loading={isSubmitting}
              disabled={isSubmitting}
              onClick={() => {
                void handleVisibilityConfirm();
              }}
            >
              Save Changes
            </Button>
          </Group>
        </Stack>
      </Modal>

      <Modal opened={deleteOpened} onClose={closeDelete} title="Delete Challenge" centered>
        <Stack gap="md">
          <Text size="sm">
            Are you sure you want to delete <strong>{formValues.title}</strong>?
          </Text>
          {courseCount > 0 && (
            <Text size="sm" c="orange">
              This challenge is connected to {courseCount} course{courseCount !== 1 ? "s" : ""}.
            </Text>
          )}
          <Text size="sm" c="dimmed">
            This action cannot be undone.
          </Text>
          {deleteError && (
            <Alert color="red" title="Failed to delete challenge">
              {deleteError}
            </Alert>
          )}
          <Group justify="flex-end" gap="sm">
            <Button variant="default" onClick={closeDelete} disabled={isDeleting}>
              Cancel
            </Button>
            <Button
              color="red"
              loading={isDeleting}
              disabled={isDeleting}
              onClick={() => {
                void handleDelete();
              }}
            >
              Delete Challenge
            </Button>
          </Group>
        </Stack>
      </Modal>

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
            variant="light"
            leftSection={<IconTrash size={16} />}
            onClick={openDelete}
            radius="md"
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
            dockerImageError={dockerImageError}
            subTaskErrors={subTaskErrors}
            defaultExpandedSubTaskIndex={0}
            onCharLimitExceeded={() => charLimitToast.show()}
            onShortDescriptionErrorClear={() => setShortDescriptionError(null)}
            onDockerImageErrorClear={() => setDockerImageError(null)}
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
