"use client";

import { Group, Loader, Text, TextInput } from "@mantine/core";
import { IconSearch } from "@tabler/icons-react";

interface AdminListSearchProps {
  query: string;
  onQueryChange: (value: string) => void;
  applyQueryNow: () => void;
  loading: boolean;
}

/**
 * Shared search bar for the admin list views (courses, labs). Pairs with
 * `useAdminPagedList`, which exposes exactly these handlers.
 */
export default function AdminListSearch({
  query,
  onQueryChange,
  applyQueryNow,
  loading,
}: AdminListSearchProps) {
  return (
    <Group justify="space-between" align="flex-end" wrap="wrap">
      <Group gap="sm" wrap="wrap" style={{ flex: 1 }}>
        <TextInput
          label="Search"
          placeholder="Title / description..."
          leftSection={<IconSearch size={16} />}
          value={query}
          onChange={(e) => onQueryChange(e.currentTarget.value)}
          onKeyDown={(e) => {
            if (e.key === "Enter") {
              e.preventDefault();
              applyQueryNow();
            }
          }}
          w={{ base: "100%", sm: 420 }}
        />
      </Group>
      {loading && (
        <Group gap="xs">
          <Loader size="sm" />
          <Text size="sm" c="dimmed">
            Loading
          </Text>
        </Group>
      )}
    </Group>
  );
}
