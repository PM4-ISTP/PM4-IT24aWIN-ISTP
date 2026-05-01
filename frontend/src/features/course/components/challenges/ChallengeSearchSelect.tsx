"use client";

import { useMemo, useState } from "react";
import { TextInput } from "@mantine/core";
import { IconSearch } from "@tabler/icons-react";
import type { ListChallengeResponseDto } from "@/src/features/course/actions/challenges";
import { ChallengePickerModal } from "@/src/features/course/components/challenges/ChallengePickerModal";

export interface ChallengeSearchSelectProps {
  excludeIds: string[];
  onSelect: (challenge: ListChallengeResponseDto) => void;
}

export function ChallengeSearchSelect({ excludeIds, onSelect }: ChallengeSearchSelectProps) {
  const [opened, setOpened] = useState(false);
  const addedIds = useMemo(() => new Set(excludeIds), [excludeIds]);

  return (
    <>
      <TextInput
        placeholder="Search labs to add..."
        leftSection={<IconSearch size={16} />}
        readOnly
        onClick={() => setOpened(true)}
        style={{ cursor: "pointer" }}
        styles={{ input: { cursor: "pointer" } }}
      />

      <ChallengePickerModal
        opened={opened}
        onClose={() => setOpened(false)}
        addedIds={addedIds}
        onSelect={onSelect}
      />
    </>
  );
}
