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
  LabFormFields,
  type ChallengeFormValues,
} from "@/src/features/course/components/labs/LabFormFields";
import { LabPodPanel } from "@/src/features/lab-pod/components/LabPodPanel";
import {
  fetchChallenge,
  updateChallenge,
  deleteChallenge,
  previewVisibilityImpact,
  type LabStatusEnum,
} from "@/src/features/course/actions/labs";
import {
  toFormChallenges,
  toRequestChallenges,
  validateChallenges,
} from "@/src/features/course/utils/challenges";
import { useDockerImageCheck } from "@/src/features/course/hooks/useDockerImageCheck";
import {
  DOCKER_IMAGE_ERROR,
  DOCKER_IMAGE_PATTERN,
} from "@/src/features/course/constants/challengeConstants";

function isMoreRestrictive(oldStatus: LabStatusEnum, newStatus: LabStatusEnum): boolean {
  if (oldStatus === newStatus) return false;
  if (newStatus === "DRAFT") return true;
  if (newStatus === "PRIVATE" && oldStatus === "PUBLIC") return true;
  return false;
}

export default function EditChallenge() {
  const router = useRouter();
  const params = useParams<{ id: string }>();
  const labId = params.id;

  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);

  const [formValues, setFormValues] = useState<ChallengeFormValues>({
    title: "",
    description: "",
    status: "DRAFT",
    difficulty: "MEDIUM",
    dockerImage: "",
    containerPort: 80,
    podTtlSeconds: undefined,
    challenges: [],
  });
  const [savedDockerImage, setSavedDockerImage] = useState<string | null>(null);
  const [initialStatus, setInitialStatus] = useState<LabStatusEnum>("DRAFT");
  const [courseCount, setCourseCount] = useState(0);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState<string | null>(null);
  const [deleteOpened, { open: openDelete, close: closeDelete }] = useDisclosure(false);
  const [visibilityOpened, { open: openVisibility, close: closeVisibility }] = useDisclosure(false);
  const [visibilityImpactCount, setVisibilityImpactCount] = useState(0);
  const [titleError, setTitleError] = useState<string | null>(null);
  const [dockerImageError, setDockerImageError] = useState<string | null>(null);
  const [challengeErrors, setChallengeErrors] = useState<
    Array<Partial<Record<"title" | "description" | "flag" | "options", string>>>
  >([]);
  const [formError, setFormError] = useState<string | null>(null);
  const dockerImageCheck = useDockerImageCheck(formValues.dockerImage);

  useEffect(() => {
    async function load() {
      const result = await fetchChallenge(labId);
      if (!result.success) {
        setLoadError(result.error);
        setLoading(false);
        return;
      }

      const lab = result.data;
      const loadedStatus = lab.status ?? "DRAFT";
      const loadedDockerImage = lab.dockerImage ?? "";
      setFormValues({
        title: lab.title ?? "",
        description: lab.description ?? "",
        status: loadedStatus,
        difficulty: lab.difficulty ?? "MEDIUM",
        dockerImage: loadedDockerImage,
        containerPort: lab.containerPort ?? 80,
        podTtlSeconds: lab.podTtlSeconds ?? undefined,
        challenges: toFormChallenges(lab.challenges),
      });
      setSavedDockerImage(loadedDockerImage);
      setInitialStatus(loadedStatus);
      setCourseCount(lab.courseCount ?? 0);

      setLoading(false);
    }

    void load();
  }, [labId]);

  async function performUpdate() {
    const trimmedDockerImage = formValues.dockerImage.trim();
    const challengeValidation = validateChallenges(formValues.challenges);
    if (!challengeValidation.valid) {
      setChallengeErrors(challengeValidation.errors);
      if (challengeValidation.formError) {
        setFormError(challengeValidation.formError);
      }
      return;
    }

    setIsSubmitting(true);

    const result = await updateChallenge(labId, {
      title: formValues.title.trim(),
      description: formValues.description,
      status: formValues.status,
      difficulty: formValues.difficulty,
      dockerImage: trimmedDockerImage,
      containerPort: formValues.containerPort,
      podTtlSeconds: formValues.podTtlSeconds,
      challenges: toRequestChallenges(formValues.challenges),
    });

    setIsSubmitting(false);

    if (!result.success) {
      setFormError(result.error);
      return;
    }

    router.refresh();
    router.push("/dashboard/instructor/labs");
  }

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

    if (isMoreRestrictive(initialStatus, formValues.status)) {
      setIsSubmitting(true);
      const preview = await previewVisibilityImpact(labId, formValues.status);
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

    const result = await deleteChallenge(labId);

    setIsDeleting(false);

    if (!result.success) {
      setDeleteError(result.error);
      return;
    }

    closeDelete();
    router.refresh();
    router.push("/dashboard/instructor/labs");
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
              onClick={() => router.push("/dashboard/instructor/labs")}
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

      <Modal opened={deleteOpened} onClose={closeDelete} title="Remove Lab" centered>
        <Stack gap="md">
          <Text size="sm">
            Remove <strong>{formValues.title}</strong> from instructor dashboards? Students and
            instructors will no longer see it in active lab lists.
          </Text>
          {courseCount > 0 && (
            <Text size="sm" c="dimmed">
              This lab is currently used in {courseCount} course{courseCount !== 1 ? "s" : ""} and
              will be removed from those courses.
            </Text>
          )}
          <Text size="sm" c="dimmed">
            Soft-deleted labs are hidden from active instructor and student lists.
          </Text>
          {deleteError && (
            <Alert color="red" title="Could not remove lab" variant="light">
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
              Remove Lab
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
            Remove Lab
          </Button>
        </Group>

        <Stack gap="lg">
          <LabFormFields
            values={formValues}
            onChange={setFormValues}
            titleError={titleError}
            dockerImageError={dockerImageError}
            dockerImageCheckStatus={dockerImageCheck.status}
            dockerImageCheckMessage={dockerImageCheck.message}
            challengeErrors={challengeErrors}
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
              <LabPodPanel labId={labId} dockerImage={savedDockerImage} />
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
