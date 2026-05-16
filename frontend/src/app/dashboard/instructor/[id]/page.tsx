"use client";

import { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { useSession } from "next-auth/react";
import {
  ActionIcon,
  Alert,
  Box,
  Button,
  Container,
  Grid,
  GridCol,
  Group,
  Loader,
  Modal,
  Select,
  Stack,
  Text,
  Textarea,
  TextInput,
  Title,
} from "@mantine/core";
import { useDisclosure } from "@mantine/hooks";
import { notifications } from "@mantine/notifications";
import { IconArrowLeft, IconTrash } from "@tabler/icons-react";
import { CoursePeoplePanel } from "@/src/features/course/components/people/CoursePeoplePanel";
import MyEditor from "@/src/shared/components/MyEditor";
import { SurfaceCard } from "@/src/shared/components/SurfaceCard";
import { InstructorMultiSelect } from "@/src/features/course/components/management/InstructorMultiSelect";
import { CourseInviteCodePanel } from "@/src/features/course/components/management/CourseInviteCodePanel";
import {
  COURSE_SHORT_DESCRIPTION_MAX_CHARS,
  normalizeShortDescription,
} from "@/src/features/course/utils/courseText";
import {
  visibilityFromFlags,
  visibilityToFlags,
} from "@/src/features/course/utils/courseVisibility";
import {
  deleteCourse,
  fetchCourse,
  removeCourseParticipant,
  regenerateInviteCode,
  updateCourse,
} from "@/src/features/course/actions/courses";
import { useCourseTopicOptions } from "@/src/features/course/hooks/useCourseTopicOptions";
import type {
  CollaboratorUserResponseDto,
  CourseVisibility,
  CourseDetailResponseDto,
  InstructorRoleEnum,
} from "@/src/shared/types/course";
import {
  CourseLabManager,
  type CourseChallengeEntry,
} from "@/src/features/course/components/management/CourseLabManager";
import { updateCourseChallenges } from "@/src/features/course/actions/labs";
import BadgeDesigner, { type BadgeConfig } from "@/src/features/badge/components/BadgeDesigner";

const OWNER_ROLE: InstructorRoleEnum = "OWNER";
const COLLABORATOR_ROLE: InstructorRoleEnum = "COLLABORATOR";

function mergeUsersById(
  current: Record<string, CollaboratorUserResponseDto>,
  users: CollaboratorUserResponseDto[]
): Record<string, CollaboratorUserResponseDto> {
  const next = { ...current };
  users.forEach((user) => {
    next[user.id as string] = user; // each user always has an ID stored
  });
  return next;
}

export default function EditCourse() {
  const router = useRouter();
  const params = useParams<{ id: string }>();
  const courseId = params.id;
  const { data: session } = useSession();
  const currentUserId = session?.userId;

  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);

  const [title, setTitle] = useState("");
  const [shortDescription, setShortDescription] = useState("");
  const [description, setDescription] = useState("");
  const [visibility, setVisibility] = useState<CourseVisibility>("DRAFT");
  const [imageUrl, setImageUrl] = useState("");
  const [topic, setTopic] = useState<string | null>(null);
  const [course, setCourse] = useState<CourseDetailResponseDto | null>(null);
  const [selectedInstructors, setSelectedInstructors] = useState<string[]>([]);
  const [knownUsers, setKnownUsers] = useState<Record<string, CollaboratorUserResponseDto>>({});
  const [initialUsers, setInitialUsers] = useState<CollaboratorUserResponseDto[]>([]);
  const [courseLabs, setCourseChallenges] = useState<CourseChallengeEntry[]>([]);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState<string | null>(null);
  const [titleError, setTitleError] = useState<string | null>(null);
  const [shortDescriptionError, setShortDescriptionError] = useState<string | null>(null);
  const [formError, setFormError] = useState<string | null>(null);
  const topicOptions = useCourseTopicOptions();
  const [deleteOpened, { open: openDelete, close: closeDelete }] = useDisclosure(false);
  const shortDescriptionCharCount = shortDescription.length;

  const [mcAttemptsMode, setMcAttemptsMode] = useState<string>("UNLIMITED");
  const [inviteCode, setInviteCode] = useState<string | null>(null);
  const [isRegenerating, setIsRegenerating] = useState(false);
  const [regenerateError, setRegenerateError] = useState<string | null>(null);
  const [codeCopied, setCodeCopied] = useState(false);

  const [removeParticipantError, setRemoveParticipantError] = useState<string | null>(null);
  const [removingParticipantIds, setRemovingParticipantIds] = useState<string[]>([]);
  const [badgeConfig, setBadgeConfig] = useState<BadgeConfig | null>(null);

  function handleCollaboratorChange(newValue: string[]) {
    const ownerId = owner?.id;
    if (ownerId && newValue.includes(ownerId)) {
      notifications.show({
        id: "course-owner-as-collaborator",
        color: "orange",
        title: "Can't add owner as collaborator",
        message:
          "The course owner is already managing this course and cannot be added as a collaborator.",
      });
      setSelectedInstructors(newValue.filter((id) => id !== ownerId));
      return;
    }
    setSelectedInstructors(newValue);
  }

  useEffect(() => {
    async function load() {
      const result = await fetchCourse(courseId);
      if (!result.success) {
        setLoadError(result.error);
        setLoading(false);
        return;
      }

      const course: CourseDetailResponseDto = result.data;
      setCourse(course);
      setTitle(course.title);
      setShortDescription(course.shortDescription ?? "");
      setDescription(course.description ?? "");
      setVisibility(visibilityFromFlags(course.isPublished, course.isPrivate));
      setImageUrl(course.imageUrl ?? "");
      setTopic(course.topic ?? null);
      setInviteCode(course.inviteCode ?? null);
      setMcAttemptsMode((course.mcAttemptsMode as string) ?? "UNLIMITED");

      // Extract collaborators (not OWNER) for the multi-select
      const collaborators = course.courseInstructors.filter(
        (ci) => ci.instructorRole === COLLABORATOR_ROLE
      );
      setSelectedInstructors(collaborators.map((ci) => ci.instructor.id as string)); // each instructor always has an ID stored
      setInitialUsers(collaborators.map((ci) => ci.instructor));
      setKnownUsers(
        mergeUsersById(
          {},
          course.courseInstructors.map((courseInstructor) => courseInstructor.instructor)
        )
      );

      // Load course labs
      const cc = (course.courseLabs ?? []).map(
        (
          c: {
            labId: string;
            labTitle: string;
            difficulty: string;
            orderIndex: number;
            dueAt?: string | null;
          },
          i: number
        ) => ({
          labId: c.labId,
          labTitle: c.labTitle,
          difficulty: c.difficulty,
          orderIndex: c.orderIndex ?? i,
          dueAt: c.dueAt ?? null,
        })
      );
      setCourseChallenges(cc);

      setLoading(false);
    }

    void load();
  }, [courseId]);

  async function handleSubmit() {
    setTitleError(null);
    setShortDescriptionError(null);
    setFormError(null);

    if (!title.trim()) {
      setTitleError("Course title is required");
      return;
    }

    const normalizedShortDescription = normalizeShortDescription(shortDescription);
    if (!normalizedShortDescription) {
      setShortDescriptionError("Short description is required");
      return;
    }

    setIsSubmitting(true);

    const { isPublished, isPrivate } = visibilityToFlags(visibility);

    const result = await updateCourse(courseId, {
      title: title.trim(),
      description,
      shortDescription: normalizedShortDescription,
      isPublished,
      isPrivate,
      imageUrl: imageUrl.trim() || null,
      topic: topic,
      collaboratorIds: selectedInstructors,
      mcAttemptsMode,
    });

    if (!result.success) {
      setIsSubmitting(false);
      setFormError(result.error);
      return;
    }

    // Save course labs separately
    const challengeResult = await updateCourseChallenges(
      courseId,
      courseLabs.map((c) => ({
        labId: c.labId,
        orderIndex: c.orderIndex,
        dueAt: c.dueAt ?? undefined,
      }))
    );

    setIsSubmitting(false);

    if (!challengeResult.success) {
      setFormError(challengeResult.error);
      return;
    }

    if (badgeConfig) {
      await fetch(`/api/backend/api/v1/courses/${courseId}/badge`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          primaryColor: badgeConfig.primaryColor,
          textColor: badgeConfig.textColor,
          template: badgeConfig.template,
          badgeIcon: badgeConfig.badgeIcon,
          badgeEnabled: badgeConfig.badgeEnabled,
        }),
      }).catch(() => {});
    }

    setInviteCode(result.data.inviteCode ?? null);
    router.refresh();
    router.push("/dashboard/instructor");
  }

  async function handleDelete() {
    setIsDeleting(true);
    setDeleteError(null);

    const result = await deleteCourse(courseId);

    setIsDeleting(false);

    if (!result.success) {
      setDeleteError(result.error);
      return;
    }

    closeDelete();
    router.refresh();
    router.push("/dashboard/instructor");
  }

  async function handleRegenerate() {
    if (!isOwner) {
      setRegenerateError("Only the course owner can regenerate the invite code.");
      return;
    }

    setIsRegenerating(true);
    setRegenerateError(null);

    const result = await regenerateInviteCode(courseId);

    setIsRegenerating(false);

    if (!result.success) {
      setRegenerateError(result.error);
      return;
    }

    setInviteCode(result.data.inviteCode ?? null);
  }

  function handleCopyCode() {
    if (!inviteCode) return;
    void navigator.clipboard.writeText(inviteCode).then(() => {
      setCodeCopied(true);
      setTimeout(() => setCodeCopied(false), 2000);
    });
  }

  function handleRemoveParticipant(participantId: string) {
    if (!isOwner) {
      setRemoveParticipantError("Only the course owner can remove participants.");
      return;
    }

    if (removingParticipantIds.includes(participantId)) return;

    setRemoveParticipantError(null);
    setRemovingParticipantIds((ids) => [...ids, participantId]);

    void (async () => {
      const result = await removeCourseParticipant(courseId, participantId);

      setRemovingParticipantIds((ids) => ids.filter((id) => id !== participantId));

      if (!result.success) {
        setRemoveParticipantError(result.error);
        return;
      }

      setCourse((prev) => {
        if (!prev) return prev;
        return {
          ...prev,
          participants: prev.participants.filter((p) => p.id !== participantId),
          participantCount: Math.max(0, prev.participantCount - 1),
        };
      });
    })();
  }

  const owner =
    course?.courseInstructors.find(
      (courseInstructor) => courseInstructor.instructorRole === OWNER_ROLE
    )?.instructor ?? null;
  const isOwner = owner?.id === currentUserId;
  const collaborators = selectedInstructors
    .map((id) => knownUsers[id])
    .filter((user): user is CollaboratorUserResponseDto => Boolean(user));

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
              onClick={() => router.push("/dashboard/instructor")}
              aria-label="Back to dashboard"
            >
              <IconArrowLeft size={20} />
            </ActionIcon>
          </Group>
          <Alert color="red" title="Could not load course" variant="light">
            Something went wrong loading this course. Please go back and try again.
          </Alert>
        </Stack>
      </Container>
    );
  }

  return (
    <Container size="xl">
      <Modal opened={deleteOpened} onClose={closeDelete} title="Remove Course" centered>
        <Stack gap="md">
          <Text size="sm">
            Remove <strong>{title}</strong> from instructor dashboards? Students and instructors
            will no longer see it in active course lists.
          </Text>
          {deleteError && (
            <Alert color="red" title="Could not remove course" variant="light">
              Something went wrong. Please try again.
            </Alert>
          )}
          <Group justify="flex-end" gap="sm">
            <Button
              variant="outline"
              radius="md"
              onClick={closeDelete}
              disabled={isDeleting}
              style={{
                borderColor: "rgba(255,255,255,0.12)",
                color: "#e2e8f0",
                background: "rgba(255,255,255,0.04)",
                fontFamily: "var(--font-space-grotesk), sans-serif",
              }}
            >
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
              Remove Course
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
              onClick={() => router.push("/dashboard/instructor")}
              aria-label="Back to dashboard"
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
                Edit Course
              </Title>
              <Text size="sm" style={{ color: "#94a3b8" }}>
                Update the course details.
              </Text>
            </Stack>
          </Group>
          {isOwner && (
            <Button
              color="red"
              variant="light"
              leftSection={<IconTrash size={16} />}
              onClick={openDelete}
              radius="md"
            >
              Remove Course
            </Button>
          )}
        </Group>

        <Grid gap="xl" align="start">
          <GridCol span={{ base: 12, md: 7, lg: 8 }}>
            <SurfaceCard variant="strong" elevation="md" padding="2rem">
              <Stack gap="lg">
                <TextInput
                  label="Course Title"
                  placeholder="Enter course title"
                  value={title}
                  onChange={(e) => setTitle(e.currentTarget.value)}
                  error={titleError}
                  required
                />

                <Textarea
                  label="Short Description"
                  placeholder="Write a short summary shown on the course card and in the blue header"
                  value={shortDescription}
                  onChange={(e) => {
                    const newVal = e.currentTarget.value;
                    if (newVal.length > COURSE_SHORT_DESCRIPTION_MAX_CHARS) {
                      notifications.show({
                        id: "course-short-description-char-limit",
                        color: "orange",
                        title: "Character limit reached",
                        message: `The short description cannot exceed ${COURSE_SHORT_DESCRIPTION_MAX_CHARS} characters (including spaces).`,
                      });
                      return;
                    }
                    setShortDescription(newVal);
                    if (shortDescriptionError) {
                      setShortDescriptionError(null);
                    }
                  }}
                  error={shortDescriptionError}
                  description={`Shown on course cards and in the blue course header. ${shortDescriptionCharCount}/${COURSE_SHORT_DESCRIPTION_MAX_CHARS} characters.`}
                  autosize
                  minRows={2}
                  maxRows={4}
                  styles={{ input: { overflowY: "auto" } }}
                  required
                />

                <Select
                  label="Topic"
                  placeholder="Select a topic"
                  data={topicOptions.options}
                  value={topic}
                  onChange={setTopic}
                  clearable
                  searchable
                  disabled={topicOptions.loading}
                />

                {topicOptions.error && (
                  <Alert color="red" variant="light" title="Topics could not be loaded">
                    You can still save the course without selecting a topic.
                  </Alert>
                )}

                <TextInput
                  label="Course Image URL"
                  placeholder="https://example.com/image.jpg"
                  value={imageUrl}
                  onChange={(e) => setImageUrl(e.currentTarget.value)}
                  description="Optional thumbnail shown on the course card."
                />

                <MyEditor description={description} setDescription={setDescription} />

                <InstructorMultiSelect
                  value={selectedInstructors}
                  onChange={handleCollaboratorChange}
                  initialUsers={initialUsers}
                  onUsersLoaded={(users) => setKnownUsers((prev) => mergeUsersById(prev, users))}
                  disabled={!isOwner}
                />

                <Select
                  label="Visibility"
                  value={visibility}
                  onChange={(value) => {
                    if (value) {
                      setVisibility(value as CourseVisibility);
                    }
                  }}
                  data={[
                    { value: "DRAFT", label: "Draft (only instructors can view)" },
                    { value: "PUBLIC", label: "Public (visible in catalog)" },
                    { value: "PRIVATE", label: "Private (invite code only)" },
                  ]}
                  description="Choose exactly one state. Draft keeps it hidden, Public shows in catalog, Private is join-by-code only."
                  allowDeselect={false}
                  disabled={!isOwner}
                />

                <Select
                  label="Multiple-Choice Attempts"
                  value={mcAttemptsMode}
                  onChange={(value) => {
                    if (value) setMcAttemptsMode(value);
                  }}
                  data={[
                    {
                      value: "UNLIMITED",
                      label: "Unlimited — retry until correct (self-learning)",
                    },
                    {
                      value: "ONCE",
                      label:
                        "Once — one attempt, graded regardless of correctness (Praktikum / exam)",
                    },
                  ]}
                  description="Controls how many times students can attempt MC questions in this course."
                  allowDeselect={false}
                />

                <CourseLabManager labs={courseLabs} onChange={setCourseChallenges} />

                {isOwner && (
                  <Box
                    style={{
                      background: "rgba(255,255,255,0.03)",
                      border: "1px solid rgba(255,255,255,0.08)",
                      borderRadius: 14,
                      padding: "1.5rem",
                    }}
                  >
                    <BadgeDesigner courseId={courseId} onChange={setBadgeConfig} />
                  </Box>
                )}

                {formError && (
                  <Alert color="red" title="Could not save changes" variant="light">
                    {formError}
                  </Alert>
                )}

                <Button
                  radius="md"
                  loading={isSubmitting}
                  disabled={isSubmitting}
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
                  Save Changes
                </Button>
              </Stack>
            </SurfaceCard>
          </GridCol>

          <GridCol span={{ base: 12, md: 5, lg: 4 }}>
            <Stack gap="lg">
              <CoursePeoplePanel
                owner={owner}
                collaborators={collaborators}
                participants={course?.participants ?? []}
                canRemoveParticipants={isOwner}
                removeParticipantError={removeParticipantError}
                removingParticipantIds={removingParticipantIds}
                onRemoveParticipant={handleRemoveParticipant}
              />

              {visibility === "PRIVATE" && isOwner && (
                <CourseInviteCodePanel
                  inviteCode={inviteCode}
                  codeCopied={codeCopied}
                  regenerateError={regenerateError}
                  isRegenerating={isRegenerating}
                  onCopyCode={handleCopyCode}
                  onRegenerate={() => void handleRegenerate()}
                />
              )}
            </Stack>
          </GridCol>
        </Grid>
      </Stack>
    </Container>
  );
}
