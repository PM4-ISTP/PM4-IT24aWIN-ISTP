"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import {
  ActionIcon,
  Alert,
  Button,
  Container,
  Group,
  Select,
  Stack,
  Switch,
  Text,
  TextInput,
  Title,
} from "@mantine/core";
import { IconArrowLeft } from "@tabler/icons-react";
import MyEditor from "@/src/components/MyEditor";
import { InstructorMultiSelect } from "@/src/components/InstructorMultiSelect";
import { createCourse } from "@/src/lib/actions/courses";
import type { CourseDifficulty } from "@/src/types/course";

const TOPIC_OPTIONS = [
  { value: "Cybersecurity", label: "Cybersecurity" },
  { value: "Programming", label: "Programming" },
  { value: "Design", label: "Design" },
  { value: "Data Science", label: "Data Science" },
  { value: "Networking", label: "Networking" },
  { value: "Cloud", label: "Cloud" },
  { value: "DevOps", label: "DevOps" },
  { value: "Other", label: "Other" },
];

const DIFFICULTY_OPTIONS = [
  { value: "BEGINNER", label: "Beginner" },
  { value: "INTERMEDIATE", label: "Intermediate" },
  { value: "ADVANCED", label: "Advanced" },
];

export default function CreateCourse() {
  const router = useRouter();

  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("<p>Add a description...</p>");
  const [isPublished, setIsPublished] = useState(false);
  const [imageUrl, setImageUrl] = useState("");
  const [topic, setTopic] = useState<string | null>(null);
  const [difficulty, setDifficulty] = useState<string | null>(null);
  const [selectedInstructors, setSelectedInstructors] = useState<string[]>([]);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [titleError, setTitleError] = useState<string | null>(null);
  const [formError, setFormError] = useState<string | null>(null);

  async function handleSubmit() {
    setTitleError(null);
    setFormError(null);

    if (!title.trim()) {
      setTitleError("Course title is required");
      return;
    }

    setIsSubmitting(true);

    const result = await createCourse({
      title: title.trim(),
      description,
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
            description="Optional: paste an image URL for the course thumbnail"
          />

          <MyEditor description={description} setDescription={setDescription} />
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
    </Container>
  );
}
