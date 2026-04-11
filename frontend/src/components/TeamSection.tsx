"use client";

import { useEffect, useState } from "react";
import { AnimatedTooltip } from "@/src/components/ui/animated-tooltip";

interface TeamMember {
  name: string;
  picture: string | null;
  title: string | null;
}

export function TeamSection() {
  const [people, setPeople] = useState<{ id: number; name: string; designation: string; image: string }[]>([]);

  useEffect(() => {
    fetch("/api/public/team")
      .then((r) => r.ok ? r.json() : null)
      .then((team: TeamMember[] | null) => {
        if (!team || team.length === 0) return;
        setPeople(
          team.map((m, i) => ({
            id: i + 1,
            name: m.name,
            designation: m.title || "Software Engineer",
            image: m.picture || "",
          }))
        );
      })
      .catch(() => {});
  }, []);

  if (people.length === 0) return null;

  return (
    <div style={{ display: "flex", flexDirection: "column", alignItems: "center", gap: 12, marginTop: 8 }}>
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
