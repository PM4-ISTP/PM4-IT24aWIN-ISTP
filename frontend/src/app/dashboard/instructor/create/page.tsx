"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import {
  ActionIcon,
  Alert,
  Container,
  Group,
  Select,
  Stack,
  Text,
  Textarea,
  TextInput,
  Title,
} from "@mantine/core";
import { notifications } from "@mantine/notifications";
import { IconArrowLeft } from "@tabler/icons-react";
import MyEditor from "@/src/shared/components/MyEditor";
import { SurfaceCard } from "@/src/shared/components/SurfaceCard";
import AppButton from "@/src/shared/components/AppButton";
import {
  CourseVisibilityField,
  CourseMcAttemptsField,
} from "@/src/features/course/components/management/CourseSettingsFields";
import { InstructorMultiSelect } from "@/src/features/course/components/management/InstructorMultiSelect";
import { createCourse } from "@/src/features/course/actions/courses";
import {
  COURSE_SHORT_DESCRIPTION_MAX_CHARS,
  normalizeShortDescription,
} from "@/src/features/course/utils/courseText";
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

    const result = await createCourse({
      title: title.trim(),
      description,
      shortDescription: normalizedShortDescription,
      status: visibility,
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
    router.push(`/dashboard/instructor/${result.data.id}`);
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
              required
              description={`Shown on course cards and in the blue course header. ${shortDescriptionCharCount}/${COURSE_SHORT_DESCRIPTION_MAX_CHARS} characters.`}
              styles={{ input: { overflowY: "auto" } }}
            />

            <MyEditor
              label="Description"
              required
              description={description}
              setDescription={setDescription}
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

            <CourseVisibilityField value={visibility} onChange={setVisibility} />

            <CourseMcAttemptsField value={mcAttemptsMode} onChange={setMcAttemptsMode} />

            {formError && (
              <Alert color="red" title="Could not create course" variant="light">
                {formError}
              </Alert>
            )}

            <AppButton
              loading={isSubmitting}
              disabled={isSubmitting}
              onClick={() => {
                void handleSubmit();
              }}
            >
              Create Course
            </AppButton>
          </Stack>
        </SurfaceCard>
      </Stack>
    </Container>
  );
}
