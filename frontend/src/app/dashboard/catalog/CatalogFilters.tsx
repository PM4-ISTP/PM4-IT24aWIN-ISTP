"use client";

import { Box, Button, Group, Select, TextInput } from "@mantine/core";
import Link from "next/link";
import JoinCourseButton from "@/src/features/course/components/enrollment/JoinCourseButton";

type CatalogFiltersProps = {
  query: string;
  topic: string;
  topics: string[];
};

export default function CatalogFilters({ query, topic, topics }: CatalogFiltersProps) {
  const topicData = [{ value: "", label: "All topics" }, ...topics.map((t) => ({ value: t, label: t }))];

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
            <JoinCourseButton />
            <Button
              type="submit"
              radius="md"
              style={{
                background: "linear-gradient(90deg, #2563eb, #4f46e5)",
                border: "none",
                fontFamily: "var(--font-space-grotesk), sans-serif",
                fontWeight: 600,
                boxShadow: "0 2px 12px rgba(79,70,229,0.3)",
              }}
            >
              Search
            </Button>
            <Link href="/dashboard/catalog">
              <Button
                variant="outline"
                radius="md"
                style={{
                  borderColor: "rgba(255,255,255,0.12)",
                  color: "#e2e8f0",
                  background: "rgba(255,255,255,0.04)",
                  fontFamily: "var(--font-space-grotesk), sans-serif",
                  fontWeight: 600,
                }}
              >
                Reset
              </Button>
            </Link>
          </Group>
        </Group>
      </form>
    </Box>
  );
}

