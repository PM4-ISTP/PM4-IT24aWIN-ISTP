"use client";

import { Input, SegmentedControl, Stack, Text } from "@mantine/core";
import type { CourseVisibility } from "@/src/shared/types/course";

/**
 * Shared course-setting fields (Visibility, Multiple-Choice Attempts).
 * Used by both the create and edit course forms so the controls — colors,
 * labels and explanatory texts — stay identical.
 */

type FormVisibility = "DRAFT" | "PUBLIC" | "PRIVATE";

const VISIBILITY_COLOR: Record<FormVisibility, string> = {
  DRAFT: "gray",
  PUBLIC: "teal",
  PRIVATE: "indigo",
};

const VISIBILITY_HINT: Record<FormVisibility, string> = {
  DRAFT:
    "Only instructors can see the course. Students cannot find it in the catalog or join it — use this while you are still preparing the content.",
  PUBLIC:
    "Listed in the public catalog. Any student can find the course and enroll directly, no invite code required.",
  PRIVATE:
    "Hidden from the catalog. Students can only enroll with the course invite code that you share with them.",
};

const MC_COLOR: Record<string, string> = {
  UNLIMITED: "teal",
  ONCE: "orange",
};

const MC_HINT: Record<string, string> = {
  UNLIMITED:
    "Students can retry a multiple-choice question as often as they like until they pick the correct answer. Best for self-paced learning.",
  ONCE: "Students get a single attempt per question. The answer is graded as submitted — correct or not. Use this for graded assessments (Praktikum / exam).",
};

function FieldDescription({ children }: { children: React.ReactNode }) {
  return <Input.Description style={{ lineHeight: 1.5 }}>{children}</Input.Description>;
}

export function CourseVisibilityField({
  value,
  onChange,
  disabled,
}: {
  value: CourseVisibility;
  onChange: (value: CourseVisibility) => void;
  disabled?: boolean;
}) {
  const formValue = (
    ["DRAFT", "PUBLIC", "PRIVATE"].includes(value) ? value : "DRAFT"
  ) as FormVisibility;

  return (
    <Stack gap={4}>
      <Input.Label>Visibility</Input.Label>
      <SegmentedControl
        value={formValue}
        onChange={(v) => onChange(v as CourseVisibility)}
        color={VISIBILITY_COLOR[formValue]}
        data={[
          { value: "DRAFT", label: "Draft" },
          { value: "PUBLIC", label: "Public" },
          { value: "PRIVATE", label: "Private" },
        ]}
        fullWidth
        disabled={disabled}
      />
      <FieldDescription>
        <Text component="span" fw={700} c={VISIBILITY_COLOR[formValue]}>
          {formValue.charAt(0) + formValue.slice(1).toLowerCase()}:
        </Text>{" "}
        {VISIBILITY_HINT[formValue]}
      </FieldDescription>
    </Stack>
  );
}

export function CourseMcAttemptsField({
  value,
  onChange,
}: {
  value: string;
  onChange: (value: string) => void;
}) {
  const mcValue = value === "ONCE" ? "ONCE" : "UNLIMITED";

  return (
    <Stack gap={4}>
      <Input.Label>Multiple-Choice Attempts</Input.Label>
      <SegmentedControl
        value={mcValue}
        onChange={onChange}
        color={MC_COLOR[mcValue]}
        data={[
          { value: "UNLIMITED", label: "Unlimited" },
          { value: "ONCE", label: "Once" },
        ]}
        fullWidth
      />
      <FieldDescription>
        <Text component="span" fw={700} c={MC_COLOR[mcValue]}>
          {mcValue === "ONCE" ? "Once" : "Unlimited"}:
        </Text>{" "}
        {MC_HINT[mcValue]}
      </FieldDescription>
    </Stack>
  );
}
