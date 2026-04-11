"use client";

import { useEffect, useState } from "react";
import { AnimatedTooltip } from "@/src/components/ui/animated-tooltip";

interface TeamMember {
  name: string;
  picture: string | null;
  title: string | null;
}

function isTeamMemberArray(data: unknown): data is TeamMember[] {
  return (
    Array.isArray(data) &&
    data.every(
      (item) =>
        typeof item === "object" &&
        item !== null &&
        "name" in item &&
        typeof (item as Record<string, unknown>).name === "string",
    )
  );
}

export function TeamSection() {
  const [people, setPeople] = useState<
    { id: number; name: string; designation: string; image: string }[]
  >([]);

  useEffect(() => {
    fetch("/api/public/team")
      .then(async (r) => {
        if (!r.ok) return;
        const data: unknown = await r.json();
        if (!isTeamMemberArray(data) || data.length === 0) return;
        setPeople(
          data.map((m, i) => ({
            id: i + 1,
            name: m.name,
            designation: m.title ?? "Software Engineer",
            image: m.picture ?? "",
          })),
        );
      })
      .catch((_err: unknown) => {
        // Team section is non-critical, silently fail
      });
  }, []);

  if (people.length === 0) return null;

  return (
    <div
      style={{
        display: "flex",
        flexDirection: "column",
        alignItems: "center",
        gap: 12,
        marginTop: 8,
      }}
    >
      <p
        style={{
          color: "rgba(255,255,255,0.2)",
          fontSize: "0.7rem",
          letterSpacing: "0.12em",
          textTransform: "uppercase",
          fontFamily: "var(--font-space-grotesk), sans-serif",
          margin: 0,
        }}
      >
        Built by
      </p>
      <AnimatedTooltip items={people} />
    </div>
  );
}
