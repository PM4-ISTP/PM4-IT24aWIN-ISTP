import CourseDetails from "@/src/features/course/components/course/CourseDetails";

export default async function MyCoursePage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;

  return <CourseDetails courseId={id} backPageName="My Courses" backHref="/dashboard/courses" />;
}
