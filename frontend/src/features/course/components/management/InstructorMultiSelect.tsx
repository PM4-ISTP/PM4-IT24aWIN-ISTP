"use client";

import { useCallback, useState } from "react";
import { Loader, MultiSelect } from "@mantine/core";
import { useDebouncedCallback } from "@mantine/hooks";
import { IconSearch } from "@tabler/icons-react";
import { useApiClient } from "@/src/shared/lib/api/client";
import { springPageableSerializer } from "@/src/shared/lib/api/querySerializers";
import type { CollaboratorUserResponseDto } from "@/src/shared/types/course";

function formatCollaboratorLabel(user: CollaboratorUserResponseDto) {
  const metadata = [user.username, user.email].filter(
    (value): value is string => typeof value === "string" && value.length > 0 && value !== user.name
  );

  return metadata.length > 0 ? `${user.name} (${metadata.join(" | ")})` : user.name;
}

function isCollaboratorUser(value: unknown): value is CollaboratorUserResponseDto {
  if (typeof value !== "object" || value === null) {
    return false;
  }

  const candidate = value as {
    id?: unknown;
    name?: unknown;
    email?: unknown;
    username?: unknown;
    picture?: unknown;
    roles?: unknown;
  };
  return (
    typeof candidate.id === "string" &&
    typeof candidate.name === "string" &&
    typeof candidate.email === "string" &&
    (candidate.username === undefined ||
      candidate.username === null ||
      typeof candidate.username === "string") &&
    (candidate.picture === undefined ||
      candidate.picture === null ||
      typeof candidate.picture === "string") &&
    Array.isArray(candidate.roles) &&
    candidate.roles.every((role) => typeof role === "string")
  );
}

interface InstructorMultiSelectProps {
  value: string[];
  onChange: (value: string[]) => void;
  initialUsers?: CollaboratorUserResponseDto[];
  onUsersLoaded?: (users: CollaboratorUserResponseDto[]) => void;
  disabled?: boolean;
}

export function InstructorMultiSelect({
  value,
  onChange,
  initialUsers,
  onUsersLoaded,
  disabled = false,
}: InstructorMultiSelectProps) {
  const [searchValue, setSearchValue] = useState("");
  const [options, setOptions] = useState<CollaboratorUserResponseDto[]>(initialUsers ?? []);
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const apiClient = useApiClient();

  const fetchInstructors = useCallback(
    async (query: string) => {
      setLoading(true);
      setErrorMessage(null);
      try {
        const { data, response } = await apiClient.GET("/api/v1/users/collaborators", {
          params: {
            query: {
              ...(query ? { query } : {}),
              pageable: { page: 0, size: 20 },
            },
          },
          querySerializer: springPageableSerializer,
        });

        if (!response.ok || !data) {
          setErrorMessage("Failed to load collaborators");
          return;
        }

        const rawContent: unknown[] = data.content ?? [];
        const fetchedInstructors: CollaboratorUserResponseDto[] =
          rawContent.filter(isCollaboratorUser);
        onUsersLoaded?.(fetchedInstructors);

        setOptions((prev) => {
          const merged = new Map(prev.map((user) => [user.id, user]));

          fetchedInstructors.forEach((user) => {
            merged.set(user.id, user);
          });

          return Array.from(merged.values());
        });
      } catch {
        setErrorMessage("Failed to load collaborators");
      } finally {
        setLoading(false);
      }
    },
    [apiClient, onUsersLoaded]
  );

  const debouncedFetch = useDebouncedCallback(fetchInstructors, 300);

  const handleSearchChange = (val: string) => {
    setSearchValue(val);
    debouncedFetch(val);
  };

  const handleDropdownOpen = () => {
    if (options.length === 0) {
      void fetchInstructors("");
    }
  };

  return (
    <MultiSelect
      label="Collaborators"
      description="You are added automatically as the owner. Only admins or instructors who have already signed in can be selected."
      placeholder={value.length === 0 ? "Type a name, username, or email..." : "Add more..."}
      leftSection={loading ? <Loader size={14} /> : <IconSearch size={14} />}
      data={options.map((user) => ({
        value: user.id as string, // each user always has an ID stored
        label: formatCollaboratorLabel(user),
      }))}
      value={value}
      onChange={onChange}
      searchable
      searchValue={searchValue}
      onSearchChange={handleSearchChange}
      onDropdownOpen={handleDropdownOpen}
      nothingFoundMessage={errorMessage ?? "No collaborators found"}
      clearable
      hidePickedOptions
      disabled={disabled}
    />
  );
}
