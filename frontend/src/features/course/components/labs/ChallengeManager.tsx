"use client";

import { useEffect, useRef, useState } from "react";
import {
  ActionIcon,
  Alert,
  Badge,
  Box,
  Button,
  Collapse,
  Group,
  Input,
  NumberInput,
  Radio,
  SegmentedControl,
  Stack,
  Text,
  TextInput,
  Textarea,
  Title,
  Tooltip,
} from "@mantine/core";
import {
  IconArrowDown,
  IconArrowUp,
  IconChevronDown,
  IconChevronRight,
  IconPlus,
  IconTrash,
} from "@tabler/icons-react";
import MyEditor from "@/src/shared/components/MyEditor";

export interface ChallengeOptionFormValues {
  id?: string;
  text: string;
  isCorrect: boolean;
  orderIndex: number;
}

export interface ChallengeFormValues {
  id?: string;
  title: string;
  description: string;
  flag: string;
  orderIndex: number;
  type: "FLAG" | "MULTIPLE_CHOICE";
  points: number;
  hint: string;
  options: ChallengeOptionFormValues[];
}

export interface ChallengeManagerProps {
  challenges: ChallengeFormValues[];
  onChange: (challenges: ChallengeFormValues[]) => void;
  errors?: Array<Partial<Record<"title" | "description" | "flag" | "options", string>>>;
  defaultExpandedIndex?: number | null;
}

const FLAG_INNER_PATTERN = /^ISTP\{(.+)\}$/;
const FLAG_FORBIDDEN_CHARS = /[^A-Za-z0-9_]/g;
const COLLAPSE_UNMOUNT_DELAY_MS = 250;

function defaultOption(orderIndex: number): ChallengeOptionFormValues {
  return { text: "", isCorrect: orderIndex === 0, orderIndex };
}

export function ChallengeManager({
  challenges,
  onChange,
  errors,
  defaultExpandedIndex = null,
}: ChallengeManagerProps) {
  const [expandedIndex, setExpandedIndex] = useState<number | null>(() => {
    if (defaultExpandedIndex === null) return null;
    return defaultExpandedIndex >= 0 && defaultExpandedIndex < challenges.length
      ? defaultExpandedIndex
      : null;
  });
  const [closingIndex, setClosingIndex] = useState<number | null>(null);
  const closeTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    return () => {
      if (closeTimerRef.current) clearTimeout(closeTimerRef.current);
    };
  }, []);

  function changeExpanded(next: number | null) {
    setExpandedIndex((prev) => {
      if (prev !== null && prev !== next) {
        if (closeTimerRef.current) clearTimeout(closeTimerRef.current);
        setClosingIndex(prev);
        closeTimerRef.current = setTimeout(() => {
          setClosingIndex(null);
          closeTimerRef.current = null;
        }, COLLAPSE_UNMOUNT_DELAY_MS);
      }
      return next;
    });
  }

  function clearClosing() {
    if (closeTimerRef.current) {
      clearTimeout(closeTimerRef.current);
      closeTimerRef.current = null;
    }
    setClosingIndex(null);
  }

  function updateAt(index: number, patch: Partial<ChallengeFormValues>) {
    const next = challenges.map((st, i) => (i === index ? { ...st, ...patch } : st));
    onChange(next);
  }

  function handleAdd() {
    const newIndex = challenges.length;
    onChange([
      ...challenges,
      {
        title: "",
        description: "",
        flag: "",
        orderIndex: newIndex,
        type: "FLAG",
        points: 1,
        hint: "",
        options: [defaultOption(0), defaultOption(1)],
      },
    ]);
    changeExpanded(newIndex);
  }

  function handleRemove(index: number) {
    const next = challenges
      .filter((_, i) => i !== index)
      .map((st, i) => ({ ...st, orderIndex: i }));
    onChange(next);
    clearClosing();
    if (expandedIndex === index) {
      setExpandedIndex(null);
    } else if (expandedIndex !== null && expandedIndex > index) {
      setExpandedIndex(expandedIndex - 1);
    }
  }

  function handleMoveUp(index: number) {
    if (index === 0) return;
    const next = [...challenges];
    [next[index - 1], next[index]] = [next[index], next[index - 1]];
    onChange(next.map((st, i) => ({ ...st, orderIndex: i })));
    clearClosing();
    if (expandedIndex === index) setExpandedIndex(index - 1);
    else if (expandedIndex === index - 1) setExpandedIndex(index);
  }

  function handleMoveDown(index: number) {
    if (index === challenges.length - 1) return;
    const next = [...challenges];
    [next[index], next[index + 1]] = [next[index + 1], next[index]];
    onChange(next.map((st, i) => ({ ...st, orderIndex: i })));
    clearClosing();
    if (expandedIndex === index) setExpandedIndex(index + 1);
    else if (expandedIndex === index + 1) setExpandedIndex(index);
  }

  function handleToggleExpand(index: number) {
    changeExpanded(expandedIndex === index ? null : index);
  }

  function handleFlagInnerChange(index: number, raw: string) {
    const wrappedMatch = raw.match(FLAG_INNER_PATTERN);
    const inner = wrappedMatch ? wrappedMatch[1] : raw;
    const sanitized = inner.replace(FLAG_FORBIDDEN_CHARS, "").toUpperCase();
    updateAt(index, { flag: sanitized });
  }

  function handleTypeChange(index: number, newType: "FLAG" | "MULTIPLE_CHOICE") {
    updateAt(index, { type: newType });
  }

  function handleOptionTextChange(stIndex: number, optIndex: number, text: string) {
    const st = challenges[stIndex];
    const updatedOptions = st.options.map((o, i) => (i === optIndex ? { ...o, text } : o));
    updateAt(stIndex, { options: updatedOptions });
  }

  function handleCorrectOptionChange(stIndex: number, optIndex: number) {
    const st = challenges[stIndex];
    const updatedOptions = st.options.map((o, i) => ({ ...o, isCorrect: i === optIndex }));
    updateAt(stIndex, { options: updatedOptions });
  }

  function handleAddOption(stIndex: number) {
    const st = challenges[stIndex];
    if (st.options.length >= 4) return;
    const updatedOptions = [...st.options, defaultOption(st.options.length)];
    updateAt(stIndex, { options: updatedOptions });
  }

  function handleRemoveOption(stIndex: number, optIndex: number) {
    const st = challenges[stIndex];
    if (st.options.length <= 2) return;
    const filtered = st.options
      .filter((_, i) => i !== optIndex)
      .map((o, i) => ({ ...o, orderIndex: i }));
    const hasCorrect = filtered.some((o) => o.isCorrect);
    if (!hasCorrect && filtered.length > 0) {
      filtered[0] = { ...filtered[0], isCorrect: true };
    }
    updateAt(stIndex, { options: filtered });
  }

  const totalPoints = challenges.reduce((sum, st) => sum + (st.points || 1), 0);

  return (
    <Stack gap="md">
      <Group justify="space-between" align="center">
        <Stack gap={2}>
          <Title order={4}>Challenges</Title>
          <Text size="sm" c="dimmed">
            Each lab awards points on correct submission. A lab without a flag or options is just a
            description.
          </Text>
        </Stack>
        <Badge size="lg" variant="light">
          {totalPoints} point{totalPoints === 1 ? "" : "s"}
        </Badge>
      </Group>

      {challenges.length === 0 && <Alert color="orange">At least one lab is required.</Alert>}

      {challenges.length > 0 && (
        <Stack gap="sm">
          {challenges.map((st, index) => {
            const err = errors?.[index] ?? {};
            const isExpanded = expandedIndex === index;
            const shouldRenderBody = isExpanded || closingIndex === index;
            const hasError = Boolean(err.title || err.description || err.flag || err.options);
            const displayTitle = st.title.trim() || `Lab ${index + 1}`;
            const isFlag = st.type === "FLAG";
            const isMC = st.type === "MULTIPLE_CHOICE";

            return (
              <Box
                key={st.id ?? `local-${index}`}
                style={{
                  border: `1px solid ${
                    hasError ? "var(--mantine-color-red-6)" : "var(--mantine-color-default-border)"
                  }`,
                  borderRadius: "var(--mantine-radius-md)",
                  overflow: "hidden",
                }}
              >
                {/* Collapsed header — always visible */}
                <Box p="md" style={{ cursor: "pointer" }} onClick={() => handleToggleExpand(index)}>
                  <Group justify="space-between" align="center" wrap="nowrap">
                    <Group gap="sm" wrap="nowrap" align="center" style={{ flex: 1, minWidth: 0 }}>
                      <ActionIcon variant="transparent" size="sm" tabIndex={-1}>
                        {isExpanded ? (
                          <IconChevronDown size={16} />
                        ) : (
                          <IconChevronRight size={16} />
                        )}
                      </ActionIcon>
                      <Text size="sm" c="dimmed" fw={600} style={{ flexShrink: 0 }}>
                        #{index + 1}
                      </Text>
                      <Text size="sm" fw={600} truncate style={{ flex: 1, minWidth: 0 }}>
                        {displayTitle}
                      </Text>
                    </Group>

                    <Group
                      gap="xs"
                      wrap="nowrap"
                      align="center"
                      onClick={(e) => e.stopPropagation()}
                    >
                      <Badge size="xs" variant="light" color={isMC ? "violet" : "grape"}>
                        {isMC ? "MC" : isFlag && st.flag.trim() ? "Flag" : "Info"}
                      </Badge>
                      <Badge size="xs" variant="light" color="blue">
                        {st.points}pt
                      </Badge>
                      <Tooltip label="Move up">
                        <ActionIcon
                          variant="subtle"
                          size="sm"
                          disabled={index === 0}
                          onClick={() => handleMoveUp(index)}
                          aria-label="Move up"
                        >
                          <IconArrowUp size={14} />
                        </ActionIcon>
                      </Tooltip>
                      <Tooltip label="Move down">
                        <ActionIcon
                          variant="subtle"
                          size="sm"
                          disabled={index === challenges.length - 1}
                          onClick={() => handleMoveDown(index)}
                          aria-label="Move down"
                        >
                          <IconArrowDown size={14} />
                        </ActionIcon>
                      </Tooltip>
                      <Tooltip label="Remove">
                        <ActionIcon
                          variant="subtle"
                          size="sm"
                          color="red"
                          onClick={() => handleRemove(index)}
                          aria-label="Remove lab"
                        >
                          <IconTrash size={14} />
                        </ActionIcon>
                      </Tooltip>
                    </Group>
                  </Group>
                </Box>

                {/* Expanded body */}
                <Collapse expanded={isExpanded}>
                  {shouldRenderBody && (
                    <Box
                      p="md"
                      style={{
                        borderTop: "1px solid rgba(255,255,255,0.08)",
                        background: "rgba(255,255,255,0.02)",
                      }}
                    >
                      <Stack gap="md">
                        <TextInput
                          label="Title"
                          placeholder="Task title"
                          value={st.title}
                          onChange={(e) => updateAt(index, { title: e.currentTarget.value })}
                          error={err.title}
                          required
                        />

                        <Input.Wrapper label="Description" required error={err.description}>
                          <MyEditor
                            description={st.description}
                            setDescription={(value) => updateAt(index, { description: value })}
                          />
                        </Input.Wrapper>

                        {/* Type + Points */}
                        <Group gap="md" align="flex-end">
                          <Stack gap={4} style={{ flex: 1 }}>
                            <Input.Label>Type</Input.Label>
                            <SegmentedControl
                              value={st.type}
                              onChange={(val) => handleTypeChange(index, val)}
                              data={[
                                { label: "Flag", value: "FLAG" },
                                { label: "Multiple Choice", value: "MULTIPLE_CHOICE" },
                              ]}
                            />
                          </Stack>
                          <NumberInput
                            label="Points"
                            description="Awarded on correct answer"
                            value={st.points}
                            onChange={(val) => updateAt(index, { points: Number(val) || 1 })}
                            min={1}
                            max={100}
                            style={{ width: 130 }}
                          />
                        </Group>

                        {/* Hint */}
                        <Textarea
                          label="Hint"
                          description="Optional — shown to the student on demand."
                          placeholder="e.g. Check the HTTP response headers..."
                          value={st.hint}
                          onChange={(e) => updateAt(index, { hint: e.currentTarget.value })}
                          autosize
                          minRows={2}
                          maxRows={4}
                          styles={{ input: { overflowY: "auto" } }}
                        />

                        {/* Flag (FLAG type only) */}
                        {isFlag && (
                          <TextInput
                            label="Flag"
                            description="Leave empty if this lab has no flag to submit."
                            placeholder="flag_content_here"
                            leftSection={<Text size="sm">ISTP&#123;</Text>}
                            leftSectionWidth={52}
                            rightSection={<Text size="sm">&#125;</Text>}
                            value={st.flag}
                            onChange={(e) => handleFlagInnerChange(index, e.currentTarget.value)}
                            error={err.flag}
                          />
                        )}

                        {/* Multiple Choice options (MC type only) */}
                        {isMC && (
                          <Stack gap="sm">
                            <Group justify="space-between" align="center">
                              <Input.Label>Answer options</Input.Label>
                              <Text size="xs" c="dimmed">
                                Click the radio to mark the correct answer
                              </Text>
                            </Group>
                            {err.options && (
                              <Text size="xs" c="red">
                                {err.options}
                              </Text>
                            )}
                            <Radio.Group
                              value={String(st.options.findIndex((o) => o.isCorrect))}
                              onChange={(val) => handleCorrectOptionChange(index, Number(val))}
                            >
                              <Stack gap="xs">
                                {st.options.map((opt, optIdx) => (
                                  <Group key={optIdx} gap="xs" align="center" wrap="nowrap">
                                    <Radio value={String(optIdx)} style={{ flexShrink: 0 }} />
                                    <TextInput
                                      placeholder={`Option ${optIdx + 1}`}
                                      value={opt.text}
                                      onChange={(e) =>
                                        handleOptionTextChange(index, optIdx, e.currentTarget.value)
                                      }
                                      style={{ flex: 1 }}
                                    />
                                    <Tooltip label="Remove option">
                                      <ActionIcon
                                        variant="subtle"
                                        color="red"
                                        size="sm"
                                        disabled={st.options.length <= 2}
                                        onClick={() => handleRemoveOption(index, optIdx)}
                                        aria-label="Remove option"
                                      >
                                        <IconTrash size={13} />
                                      </ActionIcon>
                                    </Tooltip>
                                  </Group>
                                ))}
                              </Stack>
                            </Radio.Group>
                            {st.options.length < 4 && (
                              <Button
                                variant="subtle"
                                size="xs"
                                leftSection={<IconPlus size={13} />}
                                onClick={() => handleAddOption(index)}
                              >
                                Add option
                              </Button>
                            )}
                          </Stack>
                        )}
                      </Stack>
                    </Box>
                  )}
                </Collapse>
              </Box>
            );
          })}
        </Stack>
      )}

      <Button
        variant="light"
        leftSection={<IconPlus size={14} />}
        onClick={handleAdd}
        aria-label="Add lab"
      >
        Add Lab
      </Button>
    </Stack>
  );
}
