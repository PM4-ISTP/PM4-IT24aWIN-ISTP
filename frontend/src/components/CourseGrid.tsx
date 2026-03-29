"use client";

import {Group, Pagination, SimpleGrid, Stack, Text} from "@mantine/core";
import {useRouter} from "next/navigation";
import {CourseCard} from "@/src/components/CourseCard";
import type {ListCourseResponseDto} from "@/src/types/course";

interface CourseGridProps {
    courses: ListCourseResponseDto[];
    totalPages: number;
    currentPage: number;
}

export function CourseGrid({courses, totalPages, currentPage}: CourseGridProps) {
    const router = useRouter();

    function handlePageChange(page: number) {
        router.push(`?page=${page}`);
    }

    function openEdit(_id: string) {
        // TODO: navigate to /dashboard/courses/:id when edit route is implemented
    }

    if (courses.length === 0) {
        return <Text c="dimmed">No courses found.</Text>;
    }

    return (
        <Stack gap="lg">
            <SimpleGrid cols={{base: 1, sm: 2, lg: 3}} spacing="md">
                {courses.map((course) => (
                    <CourseCard
                        key={course.id}
                        {...course}
                        updatedAt={new Date(course.updatedAt).toLocaleDateString("de-CH", {
                            day: "numeric",
                            month: "short",
                            year: "numeric",
                        })}
                        onClick={openEdit}
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
