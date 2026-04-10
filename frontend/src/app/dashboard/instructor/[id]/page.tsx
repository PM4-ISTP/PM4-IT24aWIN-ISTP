"use client";

import { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { useSession } from "next-auth/react";
import {
  ActionIcon,
  Affix,
  Alert,
  Button,
  Container,
  Grid,
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
import { deleteCourse, fetchCourse, updateCourse } from "@/src/lib/actions/courses";
import { useToast } from "@/src/hooks/useToast";
import { TOPIC_OPTIONS } from "@/src/lib/courseConstants";
import type {
  CollaboratorUserResponseDto,
  CourseDetailResponseDto,
  InstructorRoleEnum,
} from "@/src/types/course";

const OWNER_ROLE: InstructorRoleEnum = "OWNER";
const COLLABORATOR_ROLE: InstructorRoleEnum = "COLLABORATOR";

function mergeUsersById(
  current: Record<string, CollaboratorUserResponseDto>,
  users: CollaboratorUserResponseDto[]
): Record<string, CollaboratorUserResponseDto> {
  const next = { ...current };
  users.forEach((user) => {
    next[user.id] = user;
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
  const [imageUrl, setImageUrl] = useState("");
  const [topic, setTopic] = useState<string | null>(null);
  const [course, setCourse] = useState<CourseDetailResponseDto | null>(null);
  const [selectedInstructors, setSelectedInstructors] = useState<string[]>([]);
  const [knownUsers, setKnownUsers] = useState<Record<string, CollaboratorUserResponseDto>>({});
  const [initialUsers, setInitialUsers] = useState<CollaboratorUserResponseDto[]>([]);
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
      setImageUrl(course.imageUrl ?? "");
      setTopic(course.topic ?? null);

      // Extract collaborators (not OWNER) for the multi-select
      const collaborators = course.courseInstructors.filter(
        (ci) => ci.instructorRole === COLLABORATOR_ROLE
      );
      setSelectedInstructors(collaborators.map((ci) => ci.instructor.id));
      setInitialUsers(collaborators.map((ci) => ci.instructor));
      setKnownUsers(
        mergeUsersById(
          {},
          course.courseInstructors.map((courseInstructor) => courseInstructor.instructor)
        )
      );

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
      imageUrl: imageUrl.trim() || null,
      topic: topic,
      collaboratorIds: selectedInstructors,
    });

    setIsSubmitting(false);

    if (!result.success) {
      setFormError(result.error);
      return;
    }

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
              <Title order={1} size="h2">
                Edit Course
              </Title>
              <Text size="sm" c="dimmed">
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
            >
              Delete Course
            </Button>
          )}
        </Group>

        <Grid gutter="xl" align="start">
          <Grid.Col span={{ base: 12, md: 7, lg: 8 }}>
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
                onUsersLoaded={(users) =>
                  setKnownUsers((prev) => mergeUsersById(prev, users))
                }
              />

              <Switch
                label="Publish Course"
                checked={isPublished}
                onChange={(e) => setIsPublished(e.currentTarget.checked)}
              />

              {formError && (
                <Alert color="red" title="Failed to update course">
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
                Save Changes
              </Button>
            </Stack>
          </Grid.Col>

          <Grid.Col span={{ base: 12, md: 5, lg: 4 }}>
            <CoursePeoplePanel
              owner={owner}
              collaborators={collaborators}
              participants={course?.participants ?? []}
            />
          </Grid.Col>
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
