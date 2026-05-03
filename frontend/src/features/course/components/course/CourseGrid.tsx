"use client";

import { Group, Pagination, SimpleGrid, Stack, Text, ThemeIcon } from "@mantine/core";
import { IconBook2 } from "@tabler/icons-react";
import { useRouter, useSearchParams } from "next/navigation";
import { CourseCard } from "@/src/features/course/components/course/CourseCard";
import type { ListCourseResponseDto } from "@/src/features/course/actions/courses";

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
    return (
      <div className="ds-empty-state">
        <ThemeIcon size={56} radius="xl" variant="light" color="blue">
          <IconBook2 size={26} />
        </ThemeIcon>
        <Stack gap={6} align="center">
          <Text fw={600} style={{ color: "#e2e8f0" }}>
            No courses found
          </Text>
          <Text size="sm" c="dimmed">
            There are no courses available right now. Check back later.
          </Text>
        </Stack>
      </div>
    );
  }

  return (
    <Stack gap="lg">
      <SimpleGrid cols={{ base: 1, sm: 2, lg: 3 }} spacing="md">
        {courses.map((course) => (
          <CourseCard
            key={course.id}
            course={course}
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
            radius="md"
          />
        </Group>
      )}
    </Stack>
  );
}
