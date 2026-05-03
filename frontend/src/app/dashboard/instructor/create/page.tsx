"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import {
  ActionIcon,
  Affix,
  Alert,
  Box,
  Button,
  Container,
  Group,
  Notification,
  Select,
  Stack,
  Text,
  Textarea,
  TextInput,
  Title,
} from "@mantine/core";
import { IconArrowLeft, IconX } from "@tabler/icons-react";
import MyEditor from "@/src/shared/components/MyEditor";
import { InstructorMultiSelect } from "@/src/features/course/components/management/InstructorMultiSelect";
import { createCourse } from "@/src/features/course/actions/courses";
import {
  COURSE_SHORT_DESCRIPTION_MAX_CHARS,
  normalizeShortDescription,
} from "@/src/features/course/utils/courseText";
import { visibilityToFlags } from "@/src/features/course/utils/courseVisibility";
import { useToast } from "@/src/shared/hooks/useToast";
import { useCourseTopicOptions } from "@/src/features/course/hooks/useCourseTopicOptions";
import type { CourseVisibility } from "@/src/shared/types/course";
import { toUserFriendlyBackendError } from "@/src/shared/lib/userFriendlyBackendError";

export default function CreateCourse() {
  const router = useRouter();

  const [title, setTitle] = useState("");
  const [shortDescription, setShortDescription] = useState("");
  const [description, setDescription] = useState("<p>Add a description...</p>");
  const [visibility, setVisibility] = useState<CourseVisibility>("DRAFT");
  const [imageUrl, setImageUrl] = useState("");
  const [topic, setTopic] = useState<string | null>(null);
  const [selectedInstructors, setSelectedInstructors] = useState<string[]>([]);
  const [mcAttemptsMode, setMcAttemptsMode] = useState<string>("UNLIMITED");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [titleError, setTitleError] = useState<string | null>(null);
  const [shortDescriptionError, setShortDescriptionError] = useState<string | null>(null);
  const [formError, setFormError] = useState<string | null>(null);
  const charLimitToast = useToast();
  const topicOptions = useCourseTopicOptions();

  const shortDescriptionCharCount = shortDescription.length;

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

    const result = await createCourse({
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

    setIsSubmitting(false);

    if (!result.success) {
      setFormError(toUserFriendlyBackendError(result.error) ?? result.error);
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
              <Title
                order={1}
                size="h2"
                style={{
                  color: "#f1f5f9",
                  fontFamily: "var(--font-space-grotesk), sans-serif",
                  fontWeight: 700,
                }}
              >
                Create Course
              </Title>
              <Text size="sm" style={{ color: "#94a3b8" }}>
                Fill in the details to create a new course.
              </Text>
            </Stack>
          </Group>
        </Group>

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
            />

            <MyEditor description={description} setDescription={setDescription} />

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
                You can still create the course without selecting a topic.
              </Alert>
            )}

            <TextInput
              label="Course Image URL"
              placeholder="https://example.com/image.jpg"
              value={imageUrl}
              onChange={(e) => setImageUrl(e.currentTarget.value)}
              description="Optional thumbnail shown on the course card."
            />

            <InstructorMultiSelect value={selectedInstructors} onChange={setSelectedInstructors} />

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
            />

            <Select
              label="Multiple-Choice Attempts"
              value={mcAttemptsMode}
              onChange={(value) => { if (value) setMcAttemptsMode(value); }}
              data={[
                { value: "UNLIMITED", label: "Unlimited — retry until correct (self-learning)" },
                { value: "ONCE", label: "Once — one attempt, graded regardless of correctness (Praktikum / exam)" },
              ]}
              description="Controls how many times students can attempt MC questions in this course."
              allowDeselect={false}
            />

            {formError && (
              <Alert color="red" title="Could not create course" variant="light">
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
              Create Course
            </Button>
          </Stack>
        </Box>
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
            The short description cannot exceed {COURSE_SHORT_DESCRIPTION_MAX_CHARS} characters
            (including spaces).
          </Notification>
        )}
      </Affix>
    </Container>
  );
}
