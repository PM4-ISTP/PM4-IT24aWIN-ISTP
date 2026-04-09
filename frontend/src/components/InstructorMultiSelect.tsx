"use client";

import { useCallback, useState } from "react";
import { Loader, MultiSelect } from "@mantine/core";
import { useDebouncedCallback } from "@mantine/hooks";
import { IconSearch } from "@tabler/icons-react";
import type { CollaboratorUserResponseDto } from "@/src/types/course";

interface ApiErrorResponse {
  error?: string;
}

function isCollaboratorUser(value: unknown): value is CollaboratorUserResponseDto {
  if (typeof value !== "object" || value === null) {
    return false;
  }

  const candidate = value as {
    id?: unknown;
    name?: unknown;
    email?: unknown;
    picture?: unknown;
    roles?: unknown;
  };
  return (
    typeof candidate.id === "string" &&
    typeof candidate.name === "string" &&
    typeof candidate.email === "string" &&
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
}

export function InstructorMultiSelect({
  value,
  onChange,
  initialUsers,
  onUsersLoaded,
}: InstructorMultiSelectProps) {
  const [searchValue, setSearchValue] = useState("");
  const [options, setOptions] = useState<CollaboratorUserResponseDto[]>(initialUsers ?? []);
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const fetchInstructors = useCallback(
    async (name: string) => {
      setLoading(true);
      setErrorMessage(null);
      try {
        const params = new URLSearchParams({
          ...(name ? { name } : {}),
          size: "20",
          page: "0",
        });

        const res = await fetch(`/api/users/instructors?${params}`);
        const data: unknown = await res.json();

        if (!res.ok) {
          const apiError =
            typeof data === "object" &&
            data !== null &&
            "error" in data &&
            typeof (data as ApiErrorResponse).error === "string"
              ? (data as ApiErrorResponse).error
              : undefined;
          const message = apiError ?? "Failed to load collaborators";
          setErrorMessage(message);
          return;
        }

        const fetchedInstructors: CollaboratorUserResponseDto[] =
          typeof data === "object" &&
          data !== null &&
          "content" in data &&
          Array.isArray((data as { content?: unknown }).content)
            ? (data as { content: unknown[] }).content.filter(isCollaboratorUser)
            : [];
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
    [onUsersLoaded]
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
      placeholder={value.length === 0 ? "Type a name to search..." : "Add more..."}
      leftSection={loading ? <Loader size={14} /> : <IconSearch size={14} />}
      data={options.map((user) => ({
        value: user.id,
        label: user.name,
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
    />
  );
}
