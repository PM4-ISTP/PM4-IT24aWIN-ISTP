"use client";

import { Input, SegmentedControl, Stack, Textarea, TextInput } from "@mantine/core";
import MyEditor from "@/src/components/MyEditor";
import type { ChallengeStatusEnum, ChallengeDifficultyEnum } from "@/src/lib/actions/challenges";
import {
  CHALLENGE_SHORT_DESCRIPTION_MAX_CHARS,
  STATUS_OPTIONS,
  DIFFICULTY_OPTIONS,
  STATUS_COLORS,
  DIFFICULTY_COLORS,
} from "@/src/lib/challengeConstants";

export interface ChallengeFormValues {
  title: string;
  shortDescription: string;
  description: string;
  status: ChallengeStatusEnum;
  difficulty: ChallengeDifficultyEnum;
}

export interface ChallengeFormFieldsProps {
  values: ChallengeFormValues;
  onChange: (values: ChallengeFormValues) => void;
  titleError?: string | null;
  shortDescriptionError?: string | null;
  onCharLimitExceeded?: () => void;
  onShortDescriptionErrorClear?: () => void;
}

export function ChallengeFormFields({
  values,
  onChange,
  titleError,
  shortDescriptionError,
  onCharLimitExceeded,
  onShortDescriptionErrorClear,
}: ChallengeFormFieldsProps) {
  const shortDescriptionCharCount = values.shortDescription.length;

  return (
    <Stack gap="lg">
      <TextInput
        label="Challenge Title"
        placeholder="Enter challenge title"
        value={values.title}
        onChange={(e) => onChange({ ...values, title: e.currentTarget.value })}
        error={titleError}
        required
      />

      <Textarea
        label="Short Description"
        placeholder="Write a short summary for this challenge"
        value={values.shortDescription}
        onChange={(e) => {
          const newVal = e.currentTarget.value;
          if (newVal.length > CHALLENGE_SHORT_DESCRIPTION_MAX_CHARS) {
            onCharLimitExceeded?.();
            return;
          }
          onChange({ ...values, shortDescription: newVal });
          if (shortDescriptionError) {
            onShortDescriptionErrorClear?.();
          }
        }}
        error={shortDescriptionError}
        description={`${shortDescriptionCharCount}/${CHALLENGE_SHORT_DESCRIPTION_MAX_CHARS} characters.`}
        autosize
        minRows={2}
        maxRows={4}
      />

      <MyEditor
        description={values.description}
        setDescription={(desc) => onChange({ ...values, description: desc })}
      />

      <Stack gap={4}>
        <Input.Label>Status</Input.Label>
        <SegmentedControl
          value={values.status}
          onChange={(val: string) => onChange({ ...values, status: val as ChallengeStatusEnum })}
          data={STATUS_OPTIONS}
          color={STATUS_COLORS[values.status]}
          fullWidth
        />
      </Stack>

      <Stack gap={4}>
        <Input.Label>Difficulty</Input.Label>
        <SegmentedControl
          value={values.difficulty}
          onChange={(val: string) => onChange({ ...values, difficulty: val as ChallengeDifficultyEnum })}
          data={DIFFICULTY_OPTIONS}
          color={DIFFICULTY_COLORS[values.difficulty]}
          fullWidth
        />
      </Stack>
    </Stack>
  );
}
