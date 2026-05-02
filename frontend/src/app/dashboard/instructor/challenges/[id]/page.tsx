"use client";

import { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import {
  ActionIcon,
  Alert,
  Box,
  Button,
  Container,
  Group,
  Loader,
  Modal,
  Paper,
  Stack,
  Text,
  Title,
} from "@mantine/core";
import { useDisclosure } from "@mantine/hooks";
import { IconArrowLeft, IconTrash } from "@tabler/icons-react";
import {
  ChallengeFormFields,
  type ChallengeFormValues,
} from "@/src/features/course/components/challenges/ChallengeFormFields";
import { ChallengePodPanel } from "@/src/features/challenge-pod/components/ChallengePodPanel";
import {
  fetchChallenge,
  updateChallenge,
  deleteChallenge,
  previewVisibilityImpact,
  type ChallengeStatusEnum,
} from "@/src/features/course/actions/challenges";
import {
  toFormSubTasks,
  toRequestSubTasks,
  validateSubTasks,
} from "@/src/features/course/utils/subTasks";
import { useDockerImageCheck } from "@/src/features/course/hooks/useDockerImageCheck";
import {
  DOCKER_IMAGE_ERROR,
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
    description: "",
    status: "DRAFT",
    difficulty: "MEDIUM",
    dockerImage: "",
    subTasks: [],
  });
  const [savedDockerImage, setSavedDockerImage] = useState<string | null>(null);
  const [initialStatus, setInitialStatus] = useState<ChallengeStatusEnum>("DRAFT");
  const [courseCount, setCourseCount] = useState(0);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState<string | null>(null);
  const [deleteOpened, { open: openDelete, close: closeDelete }] = useDisclosure(false);
  const [visibilityOpened, { open: openVisibility, close: closeVisibility }] = useDisclosure(false);
  const [visibilityImpactCount, setVisibilityImpactCount] = useState(0);
  const [titleError, setTitleError] = useState<string | null>(null);
  const [dockerImageError, setDockerImageError] = useState<string | null>(null);
  const [subTaskErrors, setSubTaskErrors] = useState<
    Array<Partial<Record<"title" | "description" | "flag" | "options", string>>>
  >([]);
  const [formError, setFormError] = useState<string | null>(null);
  const dockerImageCheck = useDockerImageCheck(formValues.dockerImage);

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
      const loadedDockerImage = challenge.dockerImage ?? "";
      setFormValues({
        title: challenge.title ?? "",
        description: challenge.description ?? "",
        status: loadedStatus,
        difficulty: challenge.difficulty ?? "MEDIUM",
        dockerImage: loadedDockerImage,
        subTasks: toFormSubTasks(challenge.subTasks),
      });
      setSavedDockerImage(loadedDockerImage);
      setInitialStatus(loadedStatus);
      setCourseCount(challenge.courseCount ?? 0);

      setLoading(false);
    }

    void load();
  }, [challengeId]);

  async function performUpdate() {
    const trimmedDockerImage = formValues.dockerImage.trim();
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
      description: formValues.description,
      status: formValues.status,
      difficulty: formValues.difficulty,
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
    setDockerImageError(null);
    setSubTaskErrors([]);
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
              aria-label="Back to labs"
            >
              <IconArrowLeft size={20} />
            </ActionIcon>
          </Group>
          <Alert color="red" title="Could not load lab" variant="light">
            Something went wrong loading this lab. Please go back and try again.
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

      <Modal opened={deleteOpened} onClose={closeDelete} title="Delete Lab" centered>
        <Stack gap="md">
          <Text size="sm">
            Are you sure you want to delete <strong>{formValues.title}</strong>?
          </Text>
          {courseCount > 0 && (
            <Text size="sm" c="orange">
              This lab is connected to {courseCount} course{courseCount !== 1 ? "s" : ""}.
            </Text>
          )}
          <Text size="sm" c="dimmed">
            This action cannot be undone.
          </Text>
          {deleteError && (
            <Alert color="red" title="Could not delete lab" variant="light">
              Something went wrong. Please try again.
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
              Delete Lab
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
                Edit Lab
              </Title>
              <Text size="sm" style={{ color: "#94a3b8" }}>
                Update the lab details.
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
            Delete Lab
          </Button>
        </Group>

        <Stack gap="lg">
          <ChallengeFormFields
            values={formValues}
            onChange={setFormValues}
            titleError={titleError}
            dockerImageError={dockerImageError}
            dockerImageCheckStatus={dockerImageCheck.status}
            dockerImageCheckMessage={dockerImageCheck.message}
            subTaskErrors={subTaskErrors}
            onDockerImageErrorClear={() => setDockerImageError(null)}
          />

          <Paper p="md" radius="md" withBorder style={{ background: "rgba(255,255,255,0.02)" }}>
            <Group justify="space-between" align="center">
              <Box>
                <Text size="sm" fw={600}>
                  Test this lab
                </Text>
                <Text size="xs" c="dimmed">
                  Start a pod to preview the lab before publishing.
                </Text>
              </Box>
              <ChallengePodPanel challengeId={challengeId} dockerImage={savedDockerImage} />
            </Group>
          </Paper>

          {formError && (
            <Alert color="red" title="Could not save changes" variant="light">
              {formError}
            </Alert>
          )}

          <Button
            radius="md"
            loading={isSubmitting}
            disabled={
              isSubmitting ||
              isDeleting ||
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
              boxShadow: "02px 12px rgba(79,70,229,0.3)",
            }}
          >
            Save Changes
          </Button>
        </Stack>
      </Stack>
    </Container>
  );
}
