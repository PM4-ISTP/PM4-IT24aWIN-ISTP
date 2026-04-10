"use client";

import { useRef, useState } from "react";
import { useRouter } from "next/navigation";
import {
  ActionIcon,
  Affix,
  Alert,
  Button,
  Container,
  Group,
  Notification,
  Select,
  Stack,
  Switch,
  Text,
  Textarea,
  TextInput,
  Title,
} from "@mantine/core";
import { IconArrowLeft, IconX } from "@tabler/icons-react";
import MyEditor from "@/src/components/MyEditor";
import { InstructorMultiSelect } from "@/src/components/InstructorMultiSelect";
import { createCourse } from "@/src/lib/actions/courses";
import {
  COURSE_SHORT_DESCRIPTION_MAX_CHARS,
  normalizeShortDescription,
} from "@/src/lib/courseText";
import { TOPIC_OPTIONS, DIFFICULTY_OPTIONS } from "@/src/lib/courseConstants";
import type { CourseDifficulty } from "@/src/types/course";

export default function CreateCourse() {
  const router = useRouter();

  const [title, setTitle] = useState("");
  const [shortDescription, setShortDescription] = useState("");
  const [description, setDescription] = useState("<p>Add a description...</p>");
  const [isPublished, setIsPublished] = useState(false);
  const [imageUrl, setImageUrl] = useState("");
  const [topic, setTopic] = useState<string | null>(null);
  const [difficulty, setDifficulty] = useState<string | null>(null);
  const [selectedInstructors, setSelectedInstructors] = useState<string[]>([]);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [titleError, setTitleError] = useState<string | null>(null);
  const [shortDescriptionError, setShortDescriptionError] = useState<string | null>(null);
  const [formError, setFormError] = useState<string | null>(null);
  const [charLimitToastVisible, setCharLimitToastVisible] = useState(false);
  const charLimitToastTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const shortDescriptionCharCount = shortDescription.length;

  function clearCharLimitToastTimeout() {
    if (charLimitToastTimeoutRef.current) {
      clearTimeout(charLimitToastTimeoutRef.current);
      charLimitToastTimeoutRef.current = null;
    }
  }

  function hideCharLimitToast() {
    clearCharLimitToastTimeout();
    setCharLimitToastVisible(false);
  }

  function showCharLimitToast() {
    clearCharLimitToastTimeout();
    setCharLimitToastVisible(true);
    charLimitToastTimeoutRef.current = setTimeout(() => {
      setCharLimitToastVisible(false);
      charLimitToastTimeoutRef.current = null;
    }, 3500);
  }

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

    const result = await createCourse({
      title: title.trim(),
      description,
      shortDescription: normalizedShortDescription,
      isPublished,
      imageUrl: imageUrl.trim() || null,
      topic: topic,
      difficulty: difficulty as CourseDifficulty | null,
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

  return (
    <Container>
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
                Create Course
              </Title>
              <Text size="sm" c="dimmed">
                Fill in the details to create a new course.
              </Text>
            </Stack>
          </Group>
        </Group>

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
                showCharLimitToast();
                return;
              }
              setShortDescription(newVal);
              if (shortDescriptionError) {
                setShortDescriptionError(null);
              }
            }}
            error={shortDescriptionError}
            description={`Shown on course cards and in the blue course header. ${shortDescriptionCharCount}/${COURSE_SHORT_DESCRIPTION_MAX_CHARS} characters used.`}
          />

          <MyEditor description={description} setDescription={setDescription} />

          <Group grow>
            <Select
              label="Topic"
              placeholder="Select a topic"
              data={TOPIC_OPTIONS}
              value={topic}
              onChange={setTopic}
              clearable
            />
            <Select
              label="Difficulty"
              placeholder="Select difficulty"
              data={DIFFICULTY_OPTIONS}
              value={difficulty}
              onChange={setDifficulty}
              clearable
            />
          </Group>

          <TextInput
            label="Course Image URL"
            placeholder="https://example.com/image.jpg"
            value={imageUrl}
            onChange={(e) => setImageUrl(e.currentTarget.value)}
            description="Optional thumbnail shown on the course card."
          />

          <InstructorMultiSelect value={selectedInstructors} onChange={setSelectedInstructors} />

          <Switch
            label="Publish Course"
            checked={isPublished}
            onChange={(e) => setIsPublished(e.currentTarget.checked)}
          />

          {formError && (
            <Alert color="red" title="Failed to create course">
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
            Create Course
          </Button>
        </Stack>
      </Stack>

      <Affix position={{ bottom: 20, right: 20 }}>
        {charLimitToastVisible && (
          <Notification
            color="orange"
            title="Character limit reached"
            onClose={hideCharLimitToast}
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
