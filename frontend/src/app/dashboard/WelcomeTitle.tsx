"use client";

import { Title } from "@mantine/core";
import { useEffect, useState } from "react";

const FULL_PREFIX = "Welcome back, ";
const CHAR_DELAY_MS = 38;

export default function WelcomeTitle({ firstName }: { firstName: string }) {
  const fullText = FULL_PREFIX + firstName + "!";
  const [visibleCount, setVisibleCount] = useState(0);

  useEffect(() => {
    setVisibleCount(0);
    let i = 0;
    const id = setInterval(() => {
      i += 1;
      setVisibleCount(i);
      if (i >= fullText.length) clearInterval(id);
    }, CHAR_DELAY_MS);
    return () => clearInterval(id);
  }, [fullText]);

  const prefixVisible = Math.min(visibleCount, FULL_PREFIX.length);
  const nameVisible = Math.max(0, visibleCount - FULL_PREFIX.length);
  const nameAndBang = firstName + "!";

  return (
    <Title
      order={1}
      style={{
        fontFamily: "var(--font-manrope), 'Manrope', sans-serif",
        fontWeight: 800,
        color: "#fff",
        fontSize: "2.6rem",
        minHeight: "2.5rem",
      }}
    >
      {FULL_PREFIX.slice(0, prefixVisible)}
      <span style={{ color: "#3B82F6" }}>
        {nameAndBang.slice(0, nameVisible)}
      </span>
      {/* blinking cursor while typing */}
      {visibleCount < fullText.length && (
        <span
          style={{
            display: "inline-block",
            width: "2px",
            height: "1.2em",
            background: "#3B82F6",
            marginLeft: 2,
            verticalAlign: "text-bottom",
            animation: "cursorBlink 0.7s step-end infinite",
          }}
        />
      )}
    </Title>
  );
}
