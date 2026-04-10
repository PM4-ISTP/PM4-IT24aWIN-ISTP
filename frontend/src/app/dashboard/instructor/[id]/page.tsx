"use client";

import { useEffect, useRef, useState } from "react";
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
  Stack,
  Switch,
  Text,
  TextInput,
  Title,
} from "@mantine/core";
import { useDisclosure } from "@mantine/hooks";
import { IconArrowLeft, IconTrash, IconX } from "@tabler/icons-react";
import { CoursePeoplePanel } from "@/src/components/CoursePeoplePanel";
import MyEditor from "@/src/components/MyEditor";
import { InstructorMultiSelect } from "@/src/components/InstructorMultiSelect";
import { deleteCourse, fetchCourse, updateCourse } from "@/src/lib/actions/courses";
import type { CollaboratorUserResponseDto, CourseDetailResponseDto } from "@/src/types/course";

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
  const [description, setDescription] = useState("");
  const [isPublished, setIsPublished] = useState(false);
  const [course, setCourse] = useState<CourseDetailResponseDto | null>(null);
  const [selectedInstructors, setSelectedInstructors] = useState<string[]>([]);
  const [knownUsers, setKnownUsers] = useState<Record<string, CollaboratorUserResponseDto>>({});
  const [initialUsers, setInitialUsers] = useState<CollaboratorUserResponseDto[]>([]);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState<string | null>(null);
  const [titleError, setTitleError] = useState<string | null>(null);
  const [formError, setFormError] = useState<string | null>(null);
  const [toastVisible, setToastVisible] = useState(false);
  const toastTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const [deleteOpened, { open: openDelete, close: closeDelete }] = useDisclosure(false);

  function clearOwnerToastTimeout() {
    if (toastTimeoutRef.current) {
      clearTimeout(toastTimeoutRef.current);
      toastTimeoutRef.current = null;
    }
  }

  function hideOwnerToast() {
    clearOwnerToastTimeout();
    setToastVisible(false);
  }

  function showOwnerToast() {
    clearOwnerToastTimeout();
    setToastVisible(true);
    toastTimeoutRef.current = setTimeout(() => {
      setToastVisible(false);
      toastTimeoutRef.current = null;
    }, 3500);
  }

  function handleCollaboratorChange(newValue: string[]) {
    const ownerId = owner?.id;
    if (ownerId && newValue.includes(ownerId)) {
      showOwnerToast();
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
      setDescription(course.description ?? "");
      setIsPublished(course.isPublished);

      // Extract collaborators (not OWNER) for the multi-select
      const collaborators = course.courseInstructors.filter(
        (ci) => ci.instructorRole === "COLLABORATOR"
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

  useEffect(() => clearOwnerToastTimeout, []);

  async function handleSubmit() {
    setTitleError(null);
    setFormError(null);

    if (!title.trim()) {
      setTitleError("Course title is required");
      return;
    }

    setIsSubmitting(true);

    const result = await updateCourse(courseId, {
      title: title.trim(),
      description,
      isPublished,
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
      (courseInstructor) => courseInstructor.instructorRole === "OWNER"
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
              <MyEditor description={description} setDescription={setDescription} />
              <InstructorMultiSelect
                value={selectedInstructors}
                onChange={handleCollaboratorChange}
                initialUsers={initialUsers}
                onUsersLoaded={(users) => {
                  setKnownUsers((current) => mergeUsersById(current, users));
                }}
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
            <div style={{ position: "sticky", top: "var(--mantine-spacing-xl)" }}>
              <CoursePeoplePanel owner={owner} collaborators={collaborators} />
            </div>
          </Grid.Col>
        </Grid>
      </Stack>

      {toastVisible && (
        <Affix position={{ bottom: 24, right: 24 }}>
          <Notification
            color="orange"
            title="Can't add owner as collaborator"
            icon={<IconX size={16} />}
            onClose={hideOwnerToast}
          >
            The course owner is already managing this course and cannot be added as a collaborator.
          </Notification>
        </Affix>
      )}
    </Container>
  );
}
