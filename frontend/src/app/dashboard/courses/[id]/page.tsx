import CourseInformation from "@/src/components/CourseInformation";
import { fetchCourse } from "@/src/lib/actions/courses";
import { Alert, Stack } from "@mantine/core";

export default async function CourseDetails({ params }: { params: Promise<{ id: string }> }) {
  const courseId: string = (await params).id;
  const result = await fetchCourse(courseId);

  return (
    <Stack p="xl" gap="lg">
      {result.success ? (
        <CourseInformation courseData={result.data} />
      ) : (
        <Alert color="red" title="Failed to load course">
          {result.error}
        </Alert>
      )}
    </Stack>
  );
}
