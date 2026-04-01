"use client";

import { useCallback, useState } from "react";
import { MultiSelect } from "@mantine/core";
import { useDebouncedCallback } from "@mantine/hooks";

interface Instructor {
  id: string;
  name: string;
}

interface ApiErrorResponse {
  error?: string;
}

function isInstructor(value: unknown): value is Instructor {
  if (typeof value !== "object" || value === null) {
    return false;
  }

  const candidate = value as { id?: unknown; name?: unknown };
  return typeof candidate.id === "string" && typeof candidate.name === "string";
}

interface InstructorMultiSelectProps {
  value: string[];
  onChange: (value: string[]) => void;
  initialOptions?: { value: string; label: string }[];
}

export function InstructorMultiSelect({
  value,
  onChange,
  initialOptions,
}: InstructorMultiSelectProps) {
  const [searchValue, setSearchValue] = useState("");
  const [options, setOptions] = useState<{ value: string; label: string }[]>(initialOptions ?? []);
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
          const message =
            typeof data === "object" &&
            data !== null &&
            "error" in data &&
            typeof (data as ApiErrorResponse).error === "string"
              ? (data as ApiErrorResponse).error
              : "Failed to load collaborators";
          setErrorMessage(message);
          return;
        }

        const fetchedInstructors: Instructor[] =
          typeof data === "object" &&
          data !== null &&
          "content" in data &&
          Array.isArray((data as { content?: unknown }).content)
            ? (data as { content: unknown[] }).content.filter(isInstructor)
            : [];

        setOptions((prev) => {
          const selected = prev.filter((o) => value.includes(o.value));
          const newOpts = fetchedInstructors.map((u) => ({
            value: u.id,
            label: u.name,
          }));
          return [
            ...selected,
            ...newOpts.filter((o: { value: string }) => !value.includes(o.value)),
          ];
        });
      } catch {
        setErrorMessage("Failed to load collaborators");
      } finally {
        setLoading(false);
      }
    },
    [value]
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
      placeholder="Search collaborators..."
      data={options}
      value={value}
      onChange={onChange}
      searchable
      searchValue={searchValue}
      onSearchChange={handleSearchChange}
      onDropdownOpen={handleDropdownOpen}
      nothingFoundMessage={
        loading
          ? "Loading..."
          : errorMessage ?? "No collaborators found"
      }
      clearable
      hidePickedOptions
    />
  );
}
