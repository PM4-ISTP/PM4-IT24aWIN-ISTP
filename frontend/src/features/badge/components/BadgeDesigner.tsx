"use client";

import { useEffect, useState } from "react";
import { ColorInput, Group, SimpleGrid, Stack, Switch, Text, Title } from "@mantine/core";
import BadgeSvg from "./BadgeSvg";

const BG_SWATCHES = [
  "#4f46e5",
  "#2563eb",
  "#0891b2",
  "#059669",
  "#16a34a",
  "#ca8a04",
  "#ea580c",
  "#dc2626",
  "#9333ea",
  "#db2777",
  "#1e293b",
  "#334155",
];

const TEXT_SWATCHES = [
  "#ffffff",
  "#f8fafc",
  "#f1f5f9",
  "#fef9c3",
  "#fef3c7",
  "#000000",
  "#1e293b",
  "#0f172a",
];

const ICONS = ["🏆", "⭐", "🎖️", "🚀", "🔥", "💎", "🎯", "🧠", "🌟", "⚡", "🏅", "🎓"];

export interface BadgeConfig {
  primaryColor: string;
  textColor: string;
  template: number;
  badgeIcon: string;
  courseTitle?: string;
  badgeEnabled: boolean;
}

interface Props {
  courseId: string;
  onChange: (config: BadgeConfig) => void;
}

function TemplateCard({
  templateId,
  selected,
  config,
  onSelect,
}: {
  templateId: number;
  selected: boolean;
  config: BadgeConfig;
  onSelect: () => void;
}) {
  const labels: Record<number, string> = { 1: "Circle", 2: "Hexagon", 3: "Medal" };
  return (
    <div
      onClick={onSelect}
      style={{
        cursor: "pointer",
        borderRadius: 10,
        border: selected ? "2px solid #4f46e5" : "2px solid rgba(255,255,255,0.1)",
        background: selected ? "rgba(79,70,229,0.12)" : "rgba(255,255,255,0.03)",
        padding: "10px 8px 8px",
        display: "flex",
        flexDirection: "column",
        alignItems: "center",
        gap: 6,
        transition: "border-color 0.15s, background 0.15s",
      }}
    >
      <BadgeSvg
        color={config.primaryColor}
        textColor={config.textColor}
        template={templateId}
        icon={config.badgeIcon}
        title={config.courseTitle ?? ""}
        size={80}
      />
      <Text size="xs" style={{ color: selected ? "#a5b4fc" : "#94a3b8" }} fw={selected ? 600 : 400}>
        {labels[templateId]}
      </Text>
    </div>
  );
}

export default function BadgeDesigner({ courseId, onChange }: Props) {
  const [config, setConfig] = useState<BadgeConfig>({
    primaryColor: "#4f46e5",
    textColor: "#ffffff",
    template: 1,
    badgeIcon: "🏆",
    courseTitle: "",
    badgeEnabled: true,
  });

  useEffect(() => {
    void fetch(`/api/backend/api/v1/courses/${courseId}/badge`)
      .then((r) => r.json())
      .then((data: BadgeConfig) => {
        const loaded: BadgeConfig = {
          primaryColor: data.primaryColor ?? "#4f46e5",
          textColor: data.textColor ?? "#ffffff",
          template: data.template ?? 1,
          badgeIcon: data.badgeIcon ?? "🏆",
          courseTitle: data.courseTitle ?? "",
          badgeEnabled: data.badgeEnabled ?? true,
        };
        setConfig(loaded);
        onChange(loaded);
      })
      .catch(() => {});
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [courseId]);

  const update = (patch: Partial<BadgeConfig>) => {
    setConfig((prev) => {
      const next = { ...prev, ...patch };
      onChange(next);
      return next;
    });
  };

  return (
    <Stack gap="md">
      <div>
        <Title
          order={3}
          style={{ color: "#f1f5f9", fontFamily: "var(--font-space-grotesk), sans-serif" }}
        >
          Course Badge
        </Title>
        <Text size="sm" style={{ color: "#94a3b8" }} mt={2}>
          Students earn this badge when they complete all challenges.
        </Text>
      </div>

      <Switch
        label="Award badge on completion"
        checked={config.badgeEnabled}
        onChange={(e) => update({ badgeEnabled: e.currentTarget.checked })}
        styles={{ label: { color: "#cbd5e1" } }}
      />

      <Group align="flex-start" gap="xl">
        <div style={{ flexShrink: 0 }}>
          <BadgeSvg
            color={config.primaryColor}
            textColor={config.textColor}
            template={config.template}
            icon={config.badgeIcon}
            title={config.courseTitle ?? ""}
            size={200}
          />
        </div>

        <Stack gap="md" style={{ flex: 1, minWidth: 260 }}>
          <Group grow>
            <ColorInput
              label="Background color"
              value={config.primaryColor}
              onChange={(v) => update({ primaryColor: v })}
              withEyeDropper={false}
              swatches={BG_SWATCHES}
              swatchesPerRow={6}
              format="hex"
              styles={{
                input: {
                  background: "rgba(255,255,255,0.05)",
                  color: "#f1f5f9",
                  border: "1px solid rgba(255,255,255,0.12)",
                },
              }}
            />
            <ColorInput
              label="Text color"
              value={config.textColor}
              onChange={(v) => update({ textColor: v })}
              withEyeDropper={false}
              swatches={TEXT_SWATCHES}
              swatchesPerRow={4}
              format="hex"
              styles={{
                input: {
                  background: "rgba(255,255,255,0.05)",
                  color: "#f1f5f9",
                  border: "1px solid rgba(255,255,255,0.12)",
                },
              }}
            />
          </Group>

          <div>
            <Text size="sm" fw={500} style={{ color: "#cbd5e1" }} mb={6}>
              Icon
            </Text>
            <Group gap={6}>
              {ICONS.map((ic) => (
                <button
                  key={ic}
                  onClick={() => update({ badgeIcon: ic })}
                  style={{
                    fontSize: 22,
                    background:
                      config.badgeIcon === ic ? "rgba(79,70,229,0.3)" : "rgba(255,255,255,0.05)",
                    border: config.badgeIcon === ic ? "2px solid #4f46e5" : "2px solid transparent",
                    borderRadius: 8,
                    width: 40,
                    height: 40,
                    cursor: "pointer",
                    transition: "background 0.12s, border-color 0.12s",
                  }}
                >
                  {ic}
                </button>
              ))}
            </Group>
          </div>

          <div>
            <Text size="sm" fw={500} style={{ color: "#cbd5e1" }} mb={6}>
              Shape
            </Text>
            <SimpleGrid cols={3} spacing="xs">
              {[1, 2, 3].map((t) => (
                <TemplateCard
                  key={t}
                  templateId={t}
                  selected={config.template === t}
                  config={config}
                  onSelect={() => update({ template: t })}
                />
              ))}
            </SimpleGrid>
          </div>
        </Stack>
      </Group>
    </Stack>
  );
}
