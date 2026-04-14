import CourseDetails from "@/src/components/CourseDetails";

export default async function MyCoursePage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;

  return <CourseDetails userId={id} backPageName="My Courses" backHref="/dashboard/courses" />;
}
