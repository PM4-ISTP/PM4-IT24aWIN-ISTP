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
import { useToast } from "@/src/hooks/useToast";
import { TOPIC_OPTIONS } from "@/src/lib/courseConstants";

export default function CreateCourse() {
  const router = useRouter();

  const [title, setTitle] = useState("");
  const [shortDescription, setShortDescription] = useState("");
  const [description, setDescription] = useState("<p>Add a description...</p>");
  const [isPublished, setIsPublished] = useState(false);
  const [isPrivate, setIsPrivate] = useState(false);
  const [imageUrl, setImageUrl] = useState("");
  const [topic, setTopic] = useState<string | null>(null);
  const [selectedInstructors, setSelectedInstructors] = useState<string[]>([]);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [titleError, setTitleError] = useState<string | null>(null);
  const [shortDescriptionError, setShortDescriptionError] = useState<string | null>(null);
  const [formError, setFormError] = useState<string | null>(null);
  const charLimitToast = useToast();

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
      isPublished,
      isPrivate,
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

            <InstructorMultiSelect value={selectedInstructors} onChange={setSelectedInstructors} />

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
              description="Private courses are hidden from the catalog and can only be joined via invite code."
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

            {formError && (
              <Alert color="red" title="Failed to create course">
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
