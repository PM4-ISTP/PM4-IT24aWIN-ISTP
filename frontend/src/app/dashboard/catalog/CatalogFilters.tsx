"use client";

import { Box, Group, Select, TextInput } from "@mantine/core";
import JoinCourseButton from "@/src/features/course/components/enrollment/JoinCourseButton";
import AppButton from "@/src/shared/components/AppButton";

type CatalogFiltersProps = {
  query: string;
  topic: string;
  topics: string[];
};

export default function CatalogFilters({ query, topic, topics }: CatalogFiltersProps) {
  const topicData = [
    { value: "", label: "All topics" },
    ...topics.map((t) => ({ value: t, label: t })),
  ];

  return (
    <Box
      style={{
        background: "rgba(255,255,255,0.04)",
        border: "1px solid rgba(255,255,255,0.08)",
        borderRadius: 14,
        padding: "1.25rem 1.5rem",
        boxShadow: "0 4px 24px rgba(0,0,0,0.25)",
      }}
    >
      <form action="/dashboard/catalog" method="get">
        <Group align="flex-end" wrap="wrap">
          <TextInput
            name="query"
            label="Search courses"
            placeholder="Search by title, short description, or description"
            defaultValue={query}
            style={{ flex: 1 }}
          />

          <Select
            name="topic"
            label="Topic"
            data={topicData}
            defaultValue={topic}
            w={220}
            searchable
            comboboxProps={{ withinPortal: true, zIndex: 3000 }}
          />

          <Group gap="sm">
            <JoinCourseButton size="sm" />
            <AppButton type="submit">Search</AppButton>
            <AppButton tone="ghost" component="a" href="/dashboard/catalog">
              Reset
            </AppButton>
          </Group>
        </Group>
      </form>
    </Box>
  );
}
