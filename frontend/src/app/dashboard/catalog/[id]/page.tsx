import CourseDetails from "@/src/components/CourseDetails";

export default async function CatalogCoursePage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;

  return <CourseDetails courseId={id} backPageName="Catalog" backHref="/dashboard/catalog" />;
}
