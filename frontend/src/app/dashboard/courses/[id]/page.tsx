import { fetchCourse } from "@/src/lib/actions/courses";
import { Alert, Stack, Text } from "@mantine/core";

export default async function CourseDetails({
	params
}: {
	params: Promise<{ id: string }>
}) {
  const courseId: string = (await params).id;
  const result = await fetchCourse(courseId);

  return (
    <Stack p="xl" gap="lg">
      {result.success ? (
        <Text>Not implemented yet</Text>
      ) : (
        <Alert color="red" title="Failed to load course">
          {result.error}
        </Alert>
      )}
    </Stack>
  );
}