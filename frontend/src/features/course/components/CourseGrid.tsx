"use client";

import { Group, Pagination, SimpleGrid, Stack, Text } from "@mantine/core";
import { useRouter, useSearchParams } from "next/navigation";
import { CourseCard } from "@/src/features/course/components/CourseCard";
import type { ListCourseResponseDto } from "@/src/shared/types/course";

interface CourseGridProps {
  courses: ListCourseResponseDto[];
  totalPages: number;
  currentPage: number;
  coursePathPrefix?: string;
}

export function CourseGrid({
  courses,
  totalPages,
  currentPage,
  coursePathPrefix,
}: CourseGridProps) {
  const router = useRouter();
  const searchParams = useSearchParams();

  function handlePageChange(page: number) {
    const params = new URLSearchParams(searchParams.toString());
    params.set("page", page.toString());
    router.push(`?${params.toString()}`);
  }

  function handleCourseOpen(id: string) {
    if (!coursePathPrefix) {
      return;
    }

    router.push(`${coursePathPrefix}/${id}`);
  }

  if (courses.length === 0) {
    return <Text c="dimmed">No courses found.</Text>;
  }

  return (
    <Stack gap="lg">
      <SimpleGrid cols={{ base: 1, sm: 2, lg: 3 }} spacing="md">
        {courses.map((course) => (
          <CourseCard
            key={course.id}
            {...course}
            updatedAt={new Date(course.updatedAt).toLocaleDateString("de-CH", {
              day: "numeric",
              month: "short",
              year: "numeric",
            })}
            onClick={coursePathPrefix ? handleCourseOpen : undefined}
          />
        ))}
      </SimpleGrid>

      {totalPages > 1 && (
        <Group justify="center">
          <Pagination
            total={totalPages}
            value={currentPage}
            onChange={handlePageChange}
            size="sm"
          />
        </Group>
      )}
    </Stack>
  );
}
