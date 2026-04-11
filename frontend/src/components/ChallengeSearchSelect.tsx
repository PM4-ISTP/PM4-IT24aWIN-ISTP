"use client";

import { useCallback, useState } from "react";
import { Loader, Select } from "@mantine/core";
import { useDebouncedCallback } from "@mantine/hooks";
import { IconSearch } from "@tabler/icons-react";
import { searchChallenges, type ListChallengeResponseDto } from "@/src/lib/actions/challenges";

function formatChallengeLabel(challenge: ListChallengeResponseDto): string {
  return `${challenge.title} (${challenge.difficulty} | ${challenge.status} | by ${challenge.creatorName})`;
}

export interface ChallengeSearchSelectProps {
  excludeIds: string[];
  onSelect: (challenge: ListChallengeResponseDto) => void;
}

export function ChallengeSearchSelect({ excludeIds, onSelect }: ChallengeSearchSelectProps) {
  const [searchValue, setSearchValue] = useState("");
  const [options, setOptions] = useState<ListChallengeResponseDto[]>([]);
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const fetchResults = useCallback(async (query: string) => {
    setLoading(true);
    setErrorMessage(null);
    try {
      const result = await searchChallenges(query, 0, 20);
      if (!result.success) {
        setErrorMessage(result.error);
        return;
      }

      setOptions((prev) => {
        const merged = new Map(prev.map((c) => [c.id, c]));
        result.data.content.forEach((c) => merged.set(c.id, c));
        return Array.from(merged.values());
      });
    } catch {
      setErrorMessage("Failed to load challenges");
    } finally {
      setLoading(false);
    }
  }, []);

  const debouncedSearch = useDebouncedCallback(fetchResults, 300);

  function handleSearchChange(val: string) {
    setSearchValue(val);
    debouncedSearch(val);
  }

  function handleDropdownOpen() {
    if (options.length === 0) {
      void fetchResults("");
    }
  }

  const selectData = options
    .filter((c) => !excludeIds.includes(c.id))
    .map((c) => ({
      value: c.id,
      label: formatChallengeLabel(c),
    }));

  return (
    <Select
      placeholder="Search challenges to add..."
      leftSection={loading ? <Loader size={14} /> : <IconSearch size={14} />}
      data={selectData}
      value={null}
      onChange={(val) => {
        if (!val) return;
        const found = options.find((c) => c.id === val);
        if (found) {
          onSelect(found);
          setSearchValue("");
        }
      }}
      searchable
      searchValue={searchValue}
      onSearchChange={handleSearchChange}
      onDropdownOpen={handleDropdownOpen}
      nothingFoundMessage={errorMessage ?? "No challenges found"}
      clearable
    />
  );
}
