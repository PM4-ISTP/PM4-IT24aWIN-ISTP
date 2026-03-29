"use client";

import {useState} from "react";
import {useRouter} from "next/navigation";
import {ActionIcon, Alert, Button, Container, Group, Stack, Switch, Text, TextInput, Title,} from "@mantine/core";
import {IconArrowLeft} from "@tabler/icons-react";
import MyEditor from "@/src/components/MyEditor";
import {InstructorMultiSelect} from "@/src/components/InstructorMultiSelect";
import {createCourse} from "@/src/lib/actions/courses";

export default function CreateCourse() {
    const router = useRouter();

    const [title, setTitle] = useState("");
    const [description, setDescription] = useState("<p>Add a description...</p>");
    const [isPublished, setIsPublished] = useState(false);
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
                            <IconArrowLeft size={20}/>
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
                    <MyEditor description={description} setDescription={setDescription}/>
                    <InstructorMultiSelect value={selectedInstructors} onChange={setSelectedInstructors}/>
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
