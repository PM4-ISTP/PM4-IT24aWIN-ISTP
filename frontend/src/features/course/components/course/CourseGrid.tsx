"use client";

import {
  ActionIcon,
  Alert,
  Button,
  Group,
  Modal,
  Pagination,
  SimpleGrid,
  Stack,
  Text,
  ThemeIcon,
} from "@mantine/core";
import { IconBook2, IconTrash } from "@tabler/icons-react";
import { useRouter, useSearchParams } from "next/navigation";
import { useMemo, useState } from "react";
import { CourseCard } from "@/src/features/course/components/course/CourseCard";
import type { ListCourseResponseDto } from "@/src/features/course/actions/courses";
import { deleteCourse } from "@/src/features/course/actions/courses";

interface CourseGridProps {
  courses: ListCourseResponseDto[];
  totalPages: number;
  currentPage: number;
  coursePathPrefix?: string;
  enableRemove?: boolean;
}

export function CourseGrid({
  courses,
  totalPages,
  currentPage,
  coursePathPrefix,
  enableRemove = false,
}: CourseGridProps) {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [removeOpened, setRemoveOpened] = useState(false);
  const [removeError, setRemoveError] = useState<string | null>(null);
  const [removing, setRemoving] = useState(false);
  const [selectedCourseId, setSelectedCourseId] = useState<string | null>(null);
  const [selectedCourseTitle, setSelectedCourseTitle] = useState<string>("");

  const canRemove = useMemo(
    () => enableRemove && !!coursePathPrefix,
    [enableRemove, coursePathPrefix]
  );

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

  function openRemove(course: ListCourseResponseDto) {
    const id = course.id ?? null;
    if (!id) return;
    setSelectedCourseId(id);
    setSelectedCourseTitle(course.title ?? "this course");
    setRemoveError(null);
    setRemoveOpened(true);
  }

  async function confirmRemove() {
    if (!selectedCourseId) return;
    setRemoving(true);
    setRemoveError(null);
    const result = await deleteCourse(selectedCourseId);
    setRemoving(false);
    if (!result.success) {
      setRemoveError(result.error);
      return;
    }
    setRemoveOpened(false);
    setSelectedCourseId(null);
    router.refresh();
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
      <Modal
        opened={removeOpened}
        onClose={() => setRemoveOpened(false)}
        title="Delete Course"
        centered
      >
        <Stack gap="md">
          <Text size="sm">
            Delete <strong>{selectedCourseTitle}</strong>? Students and instructors will no longer
            see it in active course lists.
          </Text>
          {removeError ? (
            <Alert color="red" title="Could not delete course" variant="light">
              {removeError}
            </Alert>
          ) : null}
          <Group justify="flex-end">
            <Button variant="default" onClick={() => setRemoveOpened(false)} disabled={removing}>
              Cancel
            </Button>
            <Button
              color="red"
              onClick={() => void confirmRemove()}
              loading={removing}
              disabled={removing}
            >
              Delete
            </Button>
          </Group>
        </Stack>
      </Modal>
      <SimpleGrid cols={{ base: 1, sm: 2, lg: 3 }} spacing="md">
        {courses.map((course) => (
          <div key={course.id} style={{ position: "relative" }}>
            {canRemove && (
              <ActionIcon
                variant="filled"
                color="red"
                radius="xl"
                size="md"
                aria-label="Delete course"
                style={{ position: "absolute", top: 10, right: 10, zIndex: 5 }}
                onClick={(e) => {
                  e.preventDefault();
                  e.stopPropagation();
                  openRemove(course);
                }}
              >
                <IconTrash size={16} />
              </ActionIcon>
            )}
            <CourseCard course={course} onClick={coursePathPrefix ? handleCourseOpen : undefined} />
          </div>
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
