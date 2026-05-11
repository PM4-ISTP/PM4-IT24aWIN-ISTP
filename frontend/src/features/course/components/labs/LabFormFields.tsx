"use client";

import { Input, Loader, NumberInput, SegmentedControl, Stack, TextInput } from "@mantine/core";
import { IconCheck, IconX } from "@tabler/icons-react";
import MyEditor from "@/src/shared/components/MyEditor";
import {
  ChallengeManager,
  type ChallengeFormValues as ChallengeItemFormValues,
} from "@/src/features/course/components/labs/ChallengeManager";
import type { LabStatusEnum, LabDifficultyEnum } from "@/src/features/course/actions/labs";
import {
  STATUS_OPTIONS,
  DIFFICULTY_OPTIONS,
  STATUS_COLORS,
  DIFFICULTY_COLORS,
} from "@/src/features/course/constants/challengeConstants";
import type { DockerImageCheckStatus } from "@/src/features/course/hooks/useDockerImageCheck";

export type { ChallengeItemFormValues };

export interface ChallengeFormValues {
  title: string;
  description: string;
  status: LabStatusEnum;
  difficulty: LabDifficultyEnum;
  dockerImage: string;
  containerPort: number;
  podTtlSeconds: number;
  challenges: ChallengeItemFormValues[];
}

export interface ChallengeFormFieldsProps {
  values: ChallengeFormValues;
  onChange: (values: ChallengeFormValues) => void;
  titleError?: string | null;
  dockerImageError?: string | null;
  dockerImageCheckStatus?: DockerImageCheckStatus;
  dockerImageCheckMessage?: string | null;
  challengeErrors?: Array<Partial<Record<"title" | "description" | "flag" | "options", string>>>;
  defaultExpandedChallengeIndex?: number | null;
  onDockerImageErrorClear?: () => void;
}

export function LabFormFields({
  values,
  onChange,
  titleError,
  dockerImageError,
  dockerImageCheckStatus = "idle",
  dockerImageCheckMessage,
  challengeErrors,
  defaultExpandedChallengeIndex,
  onDockerImageErrorClear,
}: ChallengeFormFieldsProps) {
  const dockerImageFeedback =
    dockerImageCheckStatus === "error" ? dockerImageCheckMessage : dockerImageError;
  const dockerImageDescription =
    dockerImageCheckStatus === "success" || dockerImageCheckStatus === "checking"
      ? dockerImageCheckMessage
      : undefined;

  return (
    <Stack gap="lg">
      <TextInput
        label="Lab Title"
        placeholder="Enter lab title"
        value={values.title}
        onChange={(e) => onChange({ ...values, title: e.currentTarget.value })}
        error={titleError}
        required
      />

      <MyEditor
        description={values.description}
        setDescription={(desc) => onChange({ ...values, description: desc })}
      />

      <TextInput
        label="Docker Image"
        placeholder="e.g. ghcr.io/school-org/lab:1.0.0"
        value={values.dockerImage}
        onChange={(e) => {
          onChange({ ...values, dockerImage: e.currentTarget.value });
          if (dockerImageError) {
            onDockerImageErrorClear?.();
          }
        }}
        error={dockerImageFeedback}
        description={dockerImageDescription}
        rightSection={
          dockerImageCheckStatus === "checking" ? (
            <Loader size="xs" />
          ) : dockerImageCheckStatus === "success" ? (
            <IconCheck size={16} color="var(--mantine-color-green-5)" />
          ) : dockerImageCheckStatus === "error" ? (
            <IconX size={16} color="var(--mantine-color-red-5)" />
          ) : undefined
        }
        required
      />

      <NumberInput
        label="Container Port"
        min={1}
        max={65535}
        clampBehavior="strict"
        value={values.containerPort}
        onChange={(value) => onChange({ ...values, containerPort: Number(value) || 80 })}
        required
      />

      <NumberInput
        label="Pod TTL (seconds)"
        description="Default is 3600 seconds (1h). Increase for heavier labs if needed."
        min={60}
        max={86400}
        clampBehavior="strict"
        value={values.podTtlSeconds}
        onChange={(value) => onChange({ ...values, podTtlSeconds: Number(value) || 3600 })}
        required
      />

      <Stack gap={4}>
        <Input.Label>Status</Input.Label>
        <SegmentedControl
          value={values.status}
          onChange={(val: string) => onChange({ ...values, status: val as LabStatusEnum })}
          data={STATUS_OPTIONS}
          color={STATUS_COLORS[values.status]}
          fullWidth
        />
      </Stack>

      <Stack gap={4}>
        <Input.Label>Difficulty</Input.Label>
        <SegmentedControl
          value={values.difficulty}
          onChange={(val: string) => onChange({ ...values, difficulty: val as LabDifficultyEnum })}
          data={DIFFICULTY_OPTIONS}
          color={DIFFICULTY_COLORS[values.difficulty]}
          fullWidth
        />
      </Stack>

      <ChallengeManager
        challenges={values.challenges}
        onChange={(challenges) => onChange({ ...values, challenges })}
        errors={challengeErrors}
        defaultExpandedIndex={defaultExpandedChallengeIndex}
      />
    </Stack>
  );
}
