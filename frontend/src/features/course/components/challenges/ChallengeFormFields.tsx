"use client";

import { Input, Loader, SegmentedControl, Stack, TextInput } from "@mantine/core";
import { IconCheck, IconX } from "@tabler/icons-react";
import MyEditor from "@/src/shared/components/MyEditor";
import {
  SubTaskManager,
  type SubTaskFormValues,
} from "@/src/features/course/components/challenges/SubTaskManager";
import type {
  ChallengeStatusEnum,
  ChallengeDifficultyEnum,
} from "@/src/features/course/actions/challenges";
import {
  STATUS_OPTIONS,
  DIFFICULTY_OPTIONS,
  STATUS_COLORS,
  DIFFICULTY_COLORS,
} from "@/src/features/course/constants/challengeConstants";
import type { DockerImageCheckStatus } from "@/src/features/course/hooks/useDockerImageCheck";

export type { SubTaskFormValues };

export interface ChallengeFormValues {
  title: string;
  description: string;
  status: ChallengeStatusEnum;
  difficulty: ChallengeDifficultyEnum;
  dockerImage: string;
  subTasks: SubTaskFormValues[];
}

export interface ChallengeFormFieldsProps {
  values: ChallengeFormValues;
  onChange: (values: ChallengeFormValues) => void;
  titleError?: string | null;
  dockerImageError?: string | null;
  dockerImageCheckStatus?: DockerImageCheckStatus;
  dockerImageCheckMessage?: string | null;
  subTaskErrors?: Array<Partial<Record<"title" | "description" | "flag" | "options", string>>>;
  defaultExpandedSubTaskIndex?: number | null;
  onDockerImageErrorClear?: () => void;
}

export function ChallengeFormFields({
  values,
  onChange,
  titleError,
  dockerImageError,
  dockerImageCheckStatus = "idle",
  dockerImageCheckMessage,
  subTaskErrors,
  defaultExpandedSubTaskIndex,
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
        placeholder="e.g. registry/image:tag"
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
          onChange={(val: string) =>
            onChange({ ...values, difficulty: val as ChallengeDifficultyEnum })
          }
          data={DIFFICULTY_OPTIONS}
          color={DIFFICULTY_COLORS[values.difficulty]}
          fullWidth
        />
      </Stack>

      <SubTaskManager
        subTasks={values.subTasks}
        onChange={(subTasks) => onChange({ ...values, subTasks })}
        errors={subTaskErrors}
        defaultExpandedIndex={defaultExpandedSubTaskIndex}
      />
    </Stack>
  );
}
