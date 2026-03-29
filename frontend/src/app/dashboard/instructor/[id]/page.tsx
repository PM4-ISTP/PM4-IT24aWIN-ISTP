"use client";

import {useEffect, useState} from "react";
import {useParams, useRouter} from "next/navigation";
import {
    ActionIcon,
    Alert,
    Button,
    Container,
    Group,
    Loader,
    Stack,
    Switch,
    Text,
    TextInput,
    Title,
} from "@mantine/core";
import {IconArrowLeft} from "@tabler/icons-react";
import MyEditor from "@/src/components/MyEditor";
import {InstructorMultiSelect} from "@/src/components/InstructorMultiSelect";
import {fetchCourse, updateCourse} from "@/src/lib/actions/courses";
import type {CourseDetailResponseDto} from "@/src/types/course";

export default function EditCourse() {
    const router = useRouter();
    const params = useParams<{ id: string }>();
    const courseId = params.id;

    const [loading, setLoading] = useState(true);
    const [loadError, setLoadError] = useState<string | null>(null);

    const [title, setTitle] = useState("");
    const [description, setDescription] = useState("");
    const [isPublished, setIsPublished] = useState(false);
    const [selectedInstructors, setSelectedInstructors] = useState<string[]>([]);
    const [initialOptions, setInitialOptions] = useState<{ value: string; label: string }[]>([]);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [titleError, setTitleError] = useState<string | null>(null);
    const [formError, setFormError] = useState<string | null>(null);

    useEffect(() => {
        async function load() {
            const result = await fetchCourse(courseId);
            if (!result.success) {
                setLoadError(result.error);
                setLoading(false);
                return;
            }

            const course: CourseDetailResponseDto = result.data;
            setTitle(course.title);
            setDescription(course.description ?? "");
            setIsPublished(course.isPublished);

            // Extract collaborators (not OWNER) for the multi-select
            const collaborators = course.courseInstructors.filter(
                (ci) => ci.instructorRole === "COLLABORATOR"
            );
            setSelectedInstructors(collaborators.map((ci) => ci.instructor.id));
            setInitialOptions(
                collaborators.map((ci) => ({
                    value: ci.instructor.id,
                    label: ci.instructor.name,
                }))
            );

            setLoading(false);
        }

        void load();
    }, [courseId]);

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

    if (loading) {
        return (
            <Container>
                <Stack p="xl" align="center">
                    <Loader/>
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
                            <IconArrowLeft size={20}/>
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
                                Edit Course
                            </Title>
                            <Text size="sm" c="dimmed">
                                Update the course details.
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
                    <InstructorMultiSelect
                        value={selectedInstructors}
                        onChange={setSelectedInstructors}
                        initialOptions={initialOptions}
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
            </Stack>
        </Container>
    );
}
