"use client";

import { useEffect, useState } from "react";
import { Button, Group, Loader, Modal, SimpleGrid, Stack, Text, Tooltip } from "@mantine/core";
import { IconMedal, IconPrinter, IconTrophy } from "@tabler/icons-react";
import { useApiClient } from "@/src/shared/lib/api/client";
import BadgeSvg from "./BadgeSvg";

type UserBadge = {
  badgeId: string;
  courseId: string;
  courseTitle: string;
  primaryColor: string;
  textColor: string;
  template: number;
  badgeIcon: string;
  earnedAt: string;
};

interface Props {
  opened: boolean;
  onClose: () => void;
  userName?: string;
}

function buildInlineSvgContent(badge: UserBadge): {
  viewBox: string;
  height: number;
  content: string;
} {
  const c = badge.primaryColor;
  const t = badge.textColor;
  const icon = badge.badgeIcon ?? "🏆";

  const lighten = (hex: string, f: number) => {
    const r = parseInt(hex.slice(1, 3), 16),
      g = parseInt(hex.slice(3, 5), 16),
      b = parseInt(hex.slice(5, 7), 16);
    return `#${Math.min(255, Math.round(r + (255 - r) * f))
      .toString(16)
      .padStart(2, "0")}${Math.min(255, Math.round(g + (255 - g) * f))
      .toString(16)
      .padStart(2, "0")}${Math.min(255, Math.round(b + (255 - b) * f))
      .toString(16)
      .padStart(2, "0")}`;
  };
  const darken = (hex: string, f: number) => {
    const r = parseInt(hex.slice(1, 3), 16),
      g = parseInt(hex.slice(3, 5), 16),
      b = parseInt(hex.slice(5, 7), 16);
    return `#${Math.max(0, Math.round(r * (1 - f)))
      .toString(16)
      .padStart(2, "0")}${Math.max(0, Math.round(g * (1 - f)))
      .toString(16)
      .padStart(2, "0")}${Math.max(0, Math.round(b * (1 - f)))
      .toString(16)
      .padStart(2, "0")}`;
  };

  const light = lighten(c, 0.3);
  const dark = darken(c, 0.4);
  const base = `<defs>
    <linearGradient id="ig" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0%" stop-color="${light}"/>
      <stop offset="100%" stop-color="${dark}"/>
    </linearGradient>
    <radialGradient id="is" cx="38%" cy="35%" r="50%">
      <stop offset="0%" stop-color="white" stop-opacity="0.22"/>
      <stop offset="100%" stop-color="white" stop-opacity="0"/>
    </radialGradient>
  </defs>`;

  if (badge.template === 2) {
    const cx = 150;
    const cy = 155;
    const r = 135;
    const hexPoints = (radius: number) =>
      Array.from({ length: 6 }, (_, i) => {
        const angle = (Math.PI / 180) * (60 * i - 30);
        return `${cx + radius * Math.cos(angle)},${cy + radius * Math.sin(angle)}`;
      }).join(" ");
    return {
      viewBox: "0 0 300 310",
      height: 124,
      content: `${base}
      <polygon points="${hexPoints(r)}" fill="url(#ig)"/>
      <polygon points="${hexPoints(r - 10)}" fill="none" stroke="${t}" stroke-width="2" stroke-opacity="0.25"/>
      <polygon points="${hexPoints(r - 22)}" fill="none" stroke="${t}" stroke-width="1" stroke-opacity="0.15"/>
      <text x="${cx}" y="${cy - 30}" text-anchor="middle" font-size="68" dominant-baseline="middle">${icon}</text>`,
    };
  }

  if (badge.template === 3) {
    const ribbonMid = lighten(c, 0.15);
    return {
      viewBox: "0 0 300 360",
      height: 144,
      content: `${base}
      <linearGradient id="mribbon" x1="0" y1="0" x2="1" y2="0">
        <stop offset="0%" stop-color="${dark}"/>
        <stop offset="50%" stop-color="${ribbonMid}"/>
        <stop offset="100%" stop-color="${dark}"/>
      </linearGradient>
      <polygon points="122,28 150,28 148,95 124,95" fill="url(#mribbon)"/>
      <polygon points="150,28 178,28 176,95 152,95" fill="url(#mribbon)"/>
      <line x1="150" y1="28" x2="150" y2="95" stroke="${t}" stroke-width="0.5" stroke-opacity="0.2"/>
      <rect x="113" y="12" width="74" height="18" rx="5" fill="${ribbonMid}" stroke="${t}" stroke-width="1" stroke-opacity="0.3"/>
      <rect x="118" y="16" width="64" height="10" rx="3" fill="none" stroke="${t}" stroke-width="0.5" stroke-opacity="0.2"/>
      <circle cx="150" cy="215" r="120" fill="url(#ig)"/>
      <circle cx="150" cy="215" r="118" fill="none" stroke="${t}" stroke-width="3" stroke-dasharray="8 5" stroke-opacity="0.45"/>
      <circle cx="150" cy="215" r="106" fill="none" stroke="${t}" stroke-width="1.5" stroke-opacity="0.2"/>
      <circle cx="150" cy="215" r="120" fill="url(#is)"/>
      <text x="150" y="190" text-anchor="middle" font-size="68" dominant-baseline="middle">${icon}</text>`,
    };
  }

  return {
    viewBox: "0 0 300 300",
    height: 120,
    content: `${base}
    <circle cx="150" cy="150" r="130" fill="url(#ig)"/>
    <circle cx="150" cy="150" r="128" fill="none" stroke="${t}" stroke-width="3" stroke-dasharray="8 5" stroke-opacity="0.4"/>
    <circle cx="150" cy="150" r="116" fill="none" stroke="${t}" stroke-width="1.5" stroke-opacity="0.2"/>
    <circle cx="150" cy="150" r="130" fill="url(#is)"/>
    <text x="150" y="155" text-anchor="middle" font-size="80" dominant-baseline="middle">${icon}</text>`,
  };
}

function printCertificate(badge: UserBadge, userName: string) {
  const earned = new Date(badge.earnedAt).toLocaleDateString("en-GB", {
    day: "numeric",
    month: "long",
    year: "numeric",
  });
  const badgeSvg = buildInlineSvgContent(badge);

  const html = `<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8"/>
  <title>Certificate - ${badge.courseTitle}</title>
  <style>
    @import url('https://fonts.googleapis.com/css2?family=Playfair+Display:wght@400;700&family=Inter:wght@300;400;600&display=swap');
    *{margin:0;padding:0;box-sizing:border-box}
    html,body{width:100%;height:100%}
    body{display:flex;align-items:center;justify-content:center;min-height:100vh;background:#f8fafc;font-family:'Inter',sans-serif}
    .cert{width:820px;min-height:580px;background:#fff;border:2px solid ${badge.primaryColor};border-radius:16px;padding:56px 64px;position:relative;box-shadow:0 8px 40px rgba(0,0,0,0.12);display:flex;flex-direction:column;align-items:center;text-align:center;gap:20px}
    .corner{position:absolute;width:60px;height:60px;border-color:${badge.primaryColor};border-style:solid;opacity:0.35}
    .corner.tl{top:16px;left:16px;border-width:3px 0 0 3px;border-radius:6px 0 0 0}
    .corner.tr{top:16px;right:16px;border-width:3px 3px 0 0;border-radius:0 6px 0 0}
    .corner.bl{bottom:16px;left:16px;border-width:0 0 3px 3px;border-radius:0 0 0 6px}
    .corner.br{bottom:16px;right:16px;border-width:0 3px 3px 0;border-radius:0 0 6px 0}
    .label{font-size:0.7rem;letter-spacing:0.18em;text-transform:uppercase;color:${badge.primaryColor};font-weight:600}
    .headline{font-family:'Playfair Display',Georgia,serif;font-size:2.6rem;font-weight:700;color:#0f172a;line-height:1.15}
    .sub{font-size:1rem;color:#64748b;font-weight:300}
    .name{font-family:'Playfair Display',Georgia,serif;font-size:2rem;color:#1e293b;border-bottom:2px solid ${badge.primaryColor};padding-bottom:8px;padding-left:40px;padding-right:40px}
    .course{font-size:1.25rem;font-weight:600;color:#1e293b}
    .date{font-size:0.875rem;color:#94a3b8}
    @page{margin:0;size:A4 landscape}
    @media print{body{background:white}.cert{box-shadow:none}}
  </style>
</head>
<body>
  <div class="cert">
    <div class="corner tl"></div><div class="corner tr"></div>
    <div class="corner bl"></div><div class="corner br"></div>
    <p class="label">Certificate of Completion</p>
    <h1 class="headline">Achievement<br/>Unlocked</h1>
    <p class="sub">This is to certify that</p>
    <p class="name">${userName}</p>
    <p class="sub">has successfully completed all labs in</p>
    <p class="course">${badge.courseTitle}</p>
    <svg viewBox="${badgeSvg.viewBox}" width="120" height="${badgeSvg.height}" xmlns="http://www.w3.org/2000/svg">
      ${badgeSvg.content}
    </svg>
    <p class="date">Awarded on ${earned}</p>
  </div>

</body>
</html>`;
  // Use an offscreen iframe instead of window.open()+document.write(), which can be blocked by
  // browser security policies and result in a blank page.
  const frame = document.createElement("iframe");
  frame.style.position = "fixed";
  frame.style.right = "0";
  frame.style.bottom = "0";
  frame.style.width = "0";
  frame.style.height = "0";
  frame.style.border = "0";
  frame.style.opacity = "0";
  frame.setAttribute("aria-hidden", "true");
  document.body.appendChild(frame);

  frame.onload = () => {
    try {
      frame.contentWindow?.focus();
      frame.contentWindow?.print();
    } finally {
      setTimeout(() => frame.remove(), 1000);
    }
  };

  frame.srcdoc = html;
}
export default function TrophyCabinet({ opened, onClose, userName = "Student" }: Props) {
  const client = useApiClient();
  const [badges, setBadges] = useState<UserBadge[] | null>(null);
  const isLoading = opened && badges === null;
  const badgeList = badges ?? [];

  useEffect(() => {
    if (!opened) return;
    void client
      .GET("/api/v1/users/me/badges")
      .then(({ data }) => {
        // Filter out badges missing required fields — the SVG renderer reads
        // primaryColor/textColor with substring() and crashes on undefined.
        const safe: UserBadge[] = (data ?? []).flatMap((dto) =>
          typeof dto.badgeId === "string" &&
          typeof dto.courseId === "string" &&
          typeof dto.courseTitle === "string" &&
          typeof dto.primaryColor === "string" &&
          typeof dto.textColor === "string" &&
          typeof dto.template === "number" &&
          typeof dto.badgeIcon === "string" &&
          typeof dto.earnedAt === "string"
            ? [
                {
                  badgeId: dto.badgeId,
                  courseId: dto.courseId,
                  courseTitle: dto.courseTitle,
                  primaryColor: dto.primaryColor,
                  textColor: dto.textColor,
                  template: dto.template,
                  badgeIcon: dto.badgeIcon,
                  earnedAt: dto.earnedAt,
                },
              ]
            : []
        );
        setBadges(safe);
      })
      .catch(() => setBadges([]));
  }, [client, opened]);

  return (
    <Modal
      opened={opened}
      onClose={onClose}
      title={
        <Group gap="sm">
          <IconTrophy size={22} color="#f1f5f9" />
          <Text
            fw={700}
            size="lg"
            style={{ color: "#f1f5f9", fontFamily: "var(--font-space-grotesk), sans-serif" }}
          >
            Trophy Cabinet
          </Text>
        </Group>
      }
      size="xl"
      radius="lg"
      styles={{
        content: { background: "#0f172a", border: "1px solid rgba(255,255,255,0.08)" },
        header: { background: "#0f172a", borderBottom: "1px solid rgba(255,255,255,0.06)" },
        close: { color: "#94a3b8" },
      }}
    >
      {isLoading ? (
        <Stack align="center" py="xl">
          <Loader color="indigo" />
          <Text size="sm" c="dimmed">
            Loading your badges...
          </Text>
        </Stack>
      ) : badgeList.length === 0 ? (
        <Stack align="center" py="xl" gap="sm">
          <IconMedal size={64} color="#94a3b8" stroke={1.5} />
          <Text fw={600} style={{ color: "#e2e8f0" }}>
            No badges yet
          </Text>
          <Text size="sm" c="dimmed" ta="center" maw={360}>
            Complete all labs in a course to earn your first badge!
          </Text>
        </Stack>
      ) : (
        <Stack gap="xl" py="sm">
          <Text size="sm" c="dimmed">
            {badgeList.length} badge{badgeList.length !== 1 ? "s" : ""} earned
          </Text>
          <SimpleGrid cols={{ base: 2, sm: 3, md: 4 }} spacing="lg">
            {badgeList.map((b) => (
              <Stack key={b.badgeId} align="center" gap="xs">
                <Tooltip
                  label={new Date(b.earnedAt).toLocaleDateString("en-GB", {
                    day: "numeric",
                    month: "long",
                    year: "numeric",
                  })}
                  withArrow
                >
                  <div style={{ cursor: "default" }}>
                    <BadgeSvg
                      color={b.primaryColor}
                      textColor={b.textColor}
                      template={b.template}
                      icon={b.badgeIcon}
                      title={b.courseTitle}
                      size={130}
                    />
                  </div>
                </Tooltip>
                <Button
                  size="xs"
                  radius="md"
                  variant="subtle"
                  onClick={() => printCertificate(b, userName)}
                  style={{ color: "#94a3b8", fontSize: "0.72rem" }}
                  leftSection={<IconPrinter size={14} />}
                >
                  Print Certificate
                </Button>
              </Stack>
            ))}
          </SimpleGrid>
        </Stack>
      )}
    </Modal>
  );
}
