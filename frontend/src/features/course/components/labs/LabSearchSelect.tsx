"use client";

import { useMemo, useState } from "react";
import { TextInput } from "@mantine/core";
import { IconSearch } from "@tabler/icons-react";
import type { ListLabResponseDto } from "@/src/features/course/actions/labs";
import { LabPickerModal } from "@/src/features/course/components/labs/LabPickerModal";

export interface ChallengeSearchSelectProps {
  excludeIds: string[];
  onSelect: (lab: ListLabResponseDto) => void;
}

export function LabSearchSelect({ excludeIds, onSelect }: ChallengeSearchSelectProps) {
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

      <LabPickerModal
        opened={opened}
        onClose={() => setOpened(false)}
        addedIds={addedIds}
        onSelect={onSelect}
      />
    </>
  );
}
