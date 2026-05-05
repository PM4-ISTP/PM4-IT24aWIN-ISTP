import type {
  ChallengeFormValues,
  ChallengeOptionFormValues,
} from "@/src/features/course/components/labs/ChallengeManager";
import type { components } from "@/src/shared/lib/api/schema";

type ChallengeRequestDto = components["schemas"]["ChallengeRequestDto"];
type ChallengeResponseDto = components["schemas"]["ChallengeResponseDto"];

const FLAG_WRAPPER = /^ISTP\{(.+)\}$/;

export function parseFlagInner(raw: string | undefined | null): string {
  if (!raw) return "";
  const match = raw.match(FLAG_WRAPPER);
  return match ? match[1] : raw;
}

export function toFormChallenges(challenges: ChallengeResponseDto[] | undefined): ChallengeFormValues[] {
  if (!challenges) return [];
  return challenges
    .slice()
    .sort((a, b) => (a.orderIndex ?? 0) - (b.orderIndex ?? 0))
    .map((st, i) => {
      const type = st.type ?? "FLAG";
      const rawOptions = st.options ?? [];
      const options: ChallengeOptionFormValues[] =
        rawOptions.length > 0
          ? rawOptions.map((o, oi) => ({
              id: o.id,
              text: o.text ?? "",
              isCorrect: o.isCorrect ?? false,
              orderIndex: o.orderIndex ?? oi,
            }))
          : [
              { text: "", isCorrect: true, orderIndex: 0 },
              { text: "", isCorrect: false, orderIndex: 1 },
            ];
      return {
        id: st.id,
        title: st.title ?? "",
        description: st.description ?? "",
        flag: parseFlagInner(st.flag),
        orderIndex: st.orderIndex ?? i,
        type,
        points: st.points ?? 1,
        hint: st.hint ?? "",
        options,
      };
    });
}

export function toRequestChallenges(challenges: ChallengeFormValues[]): ChallengeRequestDto[] {
  return challenges.map((st, i) => {
    const trimmedFlag = st.flag.trim();
    return {
      id: st.id,
      title: st.title.trim(),
      description: st.description,
      flag: st.type === "FLAG" && trimmedFlag ? `ISTP{${trimmedFlag}}` : undefined,
      orderIndex: i,
      type: st.type,
      points: st.points || 1,
      hint: st.hint.trim() || undefined,
      options:
        st.type === "MULTIPLE_CHOICE"
          ? st.options.map((o, oi) => ({
              id: o.id,
              text: o.text.trim(),
              isCorrect: o.isCorrect,
              orderIndex: oi,
            }))
          : undefined,
    };
  });
}

export interface ChallengeValidationResult {
  valid: boolean;
  errors: Array<Partial<Record<"title" | "description" | "flag" | "options", string>>>;
  formError?: string;
}

export function validateChallenges(challenges: ChallengeFormValues[]): ChallengeValidationResult {
  if (challenges.length === 0) {
    return { valid: false, errors: [], formError: "At least one lab is required" };
  }
  const errors: Array<Partial<Record<"title" | "description" | "flag" | "options", string>>> =
    challenges.map(() => ({}));
  let valid = true;
  challenges.forEach((st, i) => {
    if (!st.title.trim()) {
      errors[i].title = "Title is required";
      valid = false;
    }
    if (!st.description.replace(/<[^>]*>/g, "").trim()) {
      errors[i].description = "Description is required";
      valid = false;
    }
    if (st.type === "MULTIPLE_CHOICE") {
      const hasEmptyOption = st.options.some((o) => !o.text.trim());
      if (hasEmptyOption) {
        errors[i].options = "All options must have text";
        valid = false;
      }
      const hasCorrect = st.options.some((o) => o.isCorrect);
      if (!hasCorrect) {
        errors[i].options = "One option must be marked as correct";
        valid = false;
      }
    }
  });
  return { valid, errors };
}
