"use client";

import { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { useSession } from "next-auth/react";
import {
  ActionIcon,
  Affix,
  Alert,
  Box,
  Button,
  Container,
  Grid,
  GridCol,
  Group,
  Loader,
  Modal,
  Notification,
  Select,
  Stack,
  Switch,
  Text,
  Textarea,
  TextInput,
  Title,
} from "@mantine/core";
import { useDisclosure } from "@mantine/hooks";
import { IconArrowLeft, IconTrash, IconX } from "@tabler/icons-react";
import { CoursePeoplePanel } from "@/src/components/CoursePeoplePanel";
import MyEditor from "@/src/components/MyEditor";
import { InstructorMultiSelect } from "@/src/components/InstructorMultiSelect";
import {
  COURSE_SHORT_DESCRIPTION_MAX_CHARS,
  normalizeShortDescription,
} from "@/src/lib/courseText";
import {
  deleteCourse,
  fetchCourse,
  regenerateInviteCode,
  updateCourse,
} from "@/src/lib/actions/courses";
import { useToast } from "@/src/hooks/useToast";
import { TOPIC_OPTIONS } from "@/src/lib/courseConstants";
import type {
  CollaboratorUserResponseDto,
  CourseDetailResponseDto,
  InstructorRoleEnum,
} from "@/src/types/course";
import {
  CourseChallengeManager,
  type CourseChallengeEntry,
} from "@/src/components/CourseChallengeManager";
import { updateCourseChallenges } from "@/src/lib/actions/challenges";

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
  const [isPublished, setIsPublished] = useState(false);
  const [isPrivate, setIsPrivate] = useState(false);
  const [imageUrl, setImageUrl] = useState("");
  const [topic, setTopic] = useState<string | null>(null);
  const [course, setCourse] = useState<CourseDetailResponseDto | null>(null);
  const [selectedInstructors, setSelectedInstructors] = useState<string[]>([]);
  const [knownUsers, setKnownUsers] = useState<Record<string, CollaboratorUserResponseDto>>({});
  const [initialUsers, setInitialUsers] = useState<CollaboratorUserResponseDto[]>([]);
  const [courseChallenges, setCourseChallenges] = useState<CourseChallengeEntry[]>([]);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState<string | null>(null);
  const [titleError, setTitleError] = useState<string | null>(null);
  const [shortDescriptionError, setShortDescriptionError] = useState<string | null>(null);
  const [formError, setFormError] = useState<string | null>(null);
  const ownerToast = useToast();
  const charLimitToast = useToast();
  const [deleteOpened, { open: openDelete, close: closeDelete }] = useDisclosure(false);
  const shortDescriptionCharCount = shortDescription.length;

  const [inviteCode, setInviteCode] = useState<string | null>(null);
  const [isRegenerating, setIsRegenerating] = useState(false);
  const [regenerateError, setRegenerateError] = useState<string | null>(null);
  const [codeCopied, setCodeCopied] = useState(false);

  function handleCollaboratorChange(newValue: string[]) {
    const ownerId = owner?.id;
    if (ownerId && newValue.includes(ownerId)) {
      ownerToast.show();
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
      setIsPublished(course.isPublished);
      setIsPrivate(course.isPrivate);
      setImageUrl(course.imageUrl ?? "");
      setTopic(course.topic ?? null);
      setInviteCode(course.inviteCode ?? null);

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

      // Load course challenges
      const cc = (course.courseChallenges ?? []).map(
        (
          c: {
            challengeId: string;
            challengeTitle: string;
            difficulty: string;
            orderIndex: number;
          },
          i: number
        ) => ({
          challengeId: c.challengeId,
          challengeTitle: c.challengeTitle,
          difficulty: c.difficulty,
          orderIndex: c.orderIndex ?? i,
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

    const result = await updateCourse(courseId, {
      title: title.trim(),
      description,
      shortDescription: normalizedShortDescription,
      isPublished,
      isPrivate,
      imageUrl: imageUrl.trim() || null,
      topic: topic,
      collaboratorIds: selectedInstructors,
    });

    if (!result.success) {
      setIsSubmitting(false);
      setFormError(result.error);
      return;
    }

    // Save course challenges separately
    const challengeResult = await updateCourseChallenges(
      courseId,
      courseChallenges.map((c) => ({
        challengeId: c.challengeId,
        orderIndex: c.orderIndex,
      }))
    );

    setIsSubmitting(false);

    if (!challengeResult.success) {
      setFormError(challengeResult.error);
      return;
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
          <Alert color="red" title="Failed to load course">
            {loadError}
          </Alert>
        </Stack>
      </Container>
    );
  }

  return (
    <Container size="xl">
      <Modal opened={deleteOpened} onClose={closeDelete} title="Delete Course" centered>
        <Stack gap="md">
          <Text size="sm">
            Are you sure you want to delete <strong>{title}</strong>? This action cannot be undone.
          </Text>
          {deleteError && (
            <Alert color="red" title="Failed to delete course">
              {deleteError}
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
              Delete Course
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
              Delete Course
            </Button>
          )}
        </Group>

        <Grid gap="xl" align="start">
          <GridCol span={{ base: 12, md: 7, lg: 8 }}>
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
                      charLimitToast.show();
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
                  required
                />

                <Select
                  label="Topic"
                  placeholder="Select a topic"
                  data={TOPIC_OPTIONS}
                  value={topic}
                  onChange={setTopic}
                  clearable
                />

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
                />

                <Switch
                  label="Publish Course"
                  checked={isPublished}
                  onChange={(e) => setIsPublished(e.currentTarget.checked)}
                  size="md"
                  styles={{
                    label: { color: "#e2e8f0", fontWeight: 500 },
                    track: {
                      backgroundColor: isPublished ? "#3b82f6" : "rgba(255,255,255,0.15)",
                      borderColor: isPublished ? "#3b82f6" : "rgba(255,255,255,0.2)",
                      cursor: "pointer",
                    },
                    thumb: { backgroundColor: "#ffffff", borderColor: "transparent" },
                  }}
                />

                <Switch
                  label="Private Course (invite-code only)"
                  checked={isPrivate}
                  onChange={(e) => setIsPrivate(e.currentTarget.checked)}
                  size="md"
                  description="Private courses are hidden from catalog and can only be joined by invite code."
                  styles={{
                    label: { color: "#e2e8f0", fontWeight: 500 },
                    description: { color: "#94a3b8" },
                    track: {
                      backgroundColor: isPrivate ? "#7c3aed" : "rgba(255,255,255,0.15)",
                      borderColor: isPrivate ? "#7c3aed" : "rgba(255,255,255,0.2)",
                      cursor: "pointer",
                    },
                    thumb: { backgroundColor: "#ffffff", borderColor: "transparent" },
                  }}
                />

                <CourseChallengeManager
                  challenges={courseChallenges}
                  onChange={setCourseChallenges}
                />

                {formError && (
                  <Alert color="red" title="Failed to update course">
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
            </Box>
          </GridCol>

          <GridCol span={{ base: 12, md: 5, lg: 4 }}>
            <Stack gap="lg">
              <CoursePeoplePanel
                owner={owner}
                collaborators={collaborators}
                participants={course?.participants ?? []}
              />

              {isPublished && isPrivate && (
                <Box
                  style={{
                    background: "rgba(255,255,255,0.04)",
                    border: "1px solid rgba(255,255,255,0.08)",
                    borderRadius: 14,
                    padding: "1.5rem",
                    boxShadow: "0 4px 24px rgba(0,0,0,0.25)",
                  }}
                >
                  <Stack gap="sm">
                    <Text
                      size="sm"
                      fw={600}
                      style={{
                        color: "#94a3b8",
                        textTransform: "uppercase",
                        letterSpacing: "0.08em",
                        fontSize: "0.7rem",
                      }}
                    >
                      Invite Code
                    </Text>

                    <Group gap="xs" align="center">
                      <Text
                        style={{
                          fontFamily: "var(--font-space-grotesk), monospace",
                          fontSize: "1.6rem",
                          fontWeight: 700,
                          letterSpacing: "0.3em",
                          color: "#f1f5f9",
                          lineHeight: 1,
                        }}
                      >
                        {inviteCode ?? "—"}
                      </Text>
                      {inviteCode && (
                        <Button
                          variant="subtle"
                          size="xs"
                          radius="md"
                          onClick={handleCopyCode}
                          style={{ color: codeCopied ? "#4ade80" : "#94a3b8" }}
                          leftSection={
                            <span
                              className="material-symbols-outlined"
                              style={{
                                fontSize: "0.95rem",
                                lineHeight: 1,
                                fontVariationSettings: "'FILL' 0, 'wght' 300, 'GRAD' 0, 'opsz' 24",
                              }}
                            >
                              {codeCopied ? "check" : "content_copy"}
                            </span>
                          }
                        >
                          {codeCopied ? "Copied!" : "Copy"}
                        </Button>
                      )}
                    </Group>

                    <Text size="xs" style={{ color: "#64748b" }}>
                      Share this code with students to let them join the course directly.
                    </Text>

                    {regenerateError && (
                      <Alert color="red" variant="light" py="xs">
                        {regenerateError}
                      </Alert>
                    )}

                    {isOwner && (
                      <Button
                        variant="outline"
                        size="xs"
                        radius="md"
                        loading={isRegenerating}
                        disabled={isRegenerating}
                        onClick={() => void handleRegenerate()}
                        leftSection={
                          <span
                            className="material-symbols-outlined"
                            style={{
                              fontSize: "0.95rem",
                              lineHeight: 1,
                              fontVariationSettings: "'FILL' 0, 'wght' 300, 'GRAD' 0, 'opsz' 24",
                            }}
                          >
                            refresh
                          </span>
                        }
                        style={{
                          borderColor: "rgba(255,255,255,0.12)",
                          color: "#e2e8f0",
                          background: "rgba(255,255,255,0.04)",
                          alignSelf: "flex-start",
                        }}
                      >
                        Regenerate code
                      </Button>
                    )}
                  </Stack>
                </Box>
              )}
            </Stack>
          </GridCol>
        </Grid>
      </Stack>

      <Affix position={{ bottom: 20, right: 20 }}>
        {ownerToast.visible && (
          <Notification
            color="orange"
            title="Can't add owner as collaborator"
            onClose={ownerToast.hide}
            withCloseButton
            icon={<IconX size={18} />}
          >
            The course owner is already managing this course and cannot be added as a collaborator.
          </Notification>
        )}
        {charLimitToast.visible && (
          <Notification
            color="orange"
            title="Character limit reached"
            onClose={charLimitToast.hide}
            withCloseButton
            icon={<IconX size={18} />}
          >
            The short description cannot exceed {COURSE_SHORT_DESCRIPTION_MAX_CHARS} characters
            (including spaces).
          </Notification>
        )}
      </Affix>
    </Container>
  );
}
