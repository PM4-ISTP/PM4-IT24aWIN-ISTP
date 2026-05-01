import type { SubTaskFormValues } from "@/src/features/course/components/challenges/SubTaskManager";
import type { components } from "@/src/shared/lib/api/schema";

type SubTaskRequestDto = components["schemas"]["SubTaskRequestDto"];
type SubTaskResponseDto = components["schemas"]["SubTaskResponseDto"];

const FLAG_WRAPPER = /^ISTP\{(.+)\}$/;

export function parseFlagInner(raw: string | undefined | null): string {
  if (!raw) return "";
  const match = raw.match(FLAG_WRAPPER);
  return match ? match[1] : raw;
}

export function toFormSubTasks(subTasks: SubTaskResponseDto[] | undefined): SubTaskFormValues[] {
  if (!subTasks) return [];
  return subTasks
    .slice()
    .sort((a, b) => (a.orderIndex ?? 0) - (b.orderIndex ?? 0))
    .map((st, i) => ({
      id: st.id,
      title: st.title ?? "",
      description: st.description ?? "",
      flag: parseFlagInner(st.flag),
      orderIndex: st.orderIndex ?? i,
    }));
}

export function toRequestSubTasks(subTasks: SubTaskFormValues[]): SubTaskRequestDto[] {
  return subTasks.map((st, i) => {
    const trimmedFlag = st.flag.trim();
    return {
      id: st.id,
      title: st.title.trim(),
      description: st.description,
      flag: trimmedFlag ? `ISTP{${trimmedFlag}}` : undefined,
      orderIndex: i,
    };
  });
}

export interface SubTaskValidationResult {
  valid: boolean;
  errors: Array<Partial<Record<"title" | "description" | "flag", string>>>;
  formError?: string;
}

export function validateSubTasks(subTasks: SubTaskFormValues[]): SubTaskValidationResult {
  if (subTasks.length === 0) {
    return {
      valid: false,
      errors: [],
      formError: "At least one challenge is required",
    };
  }

  const errors: Array<Partial<Record<"title" | "description" | "flag", string>>> = subTasks.map(
    () => ({})
  );
  let valid = true;

  subTasks.forEach((st, i) => {
    if (!st.title.trim()) {
      errors[i].title = "Title is required";
      valid = false;
    }
    if (!st.description.replace(/<[^>]*>/g, "").trim()) {
      errors[i].description = "Description is required";
      valid = false;
    }
  });

  return { valid, errors };
}
