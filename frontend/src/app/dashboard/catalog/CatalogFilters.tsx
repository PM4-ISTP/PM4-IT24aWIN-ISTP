"use client";

import { Box, Flex, Group, Select, Stack, TextInput } from "@mantine/core";
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
        <Stack gap="md">
          {/* Large search input on its own row */}
          <TextInput
            name="query"
            label="Search courses"
            placeholder="Search by title, short description, or description"
            defaultValue={query}
          />

          {/* Topic filter grows to fill the space; actions sit beside it. */}
          <Flex
            direction={{ base: "column", sm: "row" }}
            align={{ base: "stretch", sm: "flex-end" }}
            gap="sm"
          >
            <Select
              name="topic"
              label="Topic"
              data={topicData}
              defaultValue={topic}
              searchable
              comboboxProps={{ withinPortal: true, zIndex: 3000 }}
              style={{ flex: 1, minWidth: 180 }}
            />
            <Group gap="sm" grow>
              <AppButton type="submit">Search</AppButton>
              <AppButton tone="ghost" component="a" href="/dashboard/catalog">
                Reset
              </AppButton>
            </Group>
          </Flex>
        </Stack>
      </form>
    </Box>
  );
}
