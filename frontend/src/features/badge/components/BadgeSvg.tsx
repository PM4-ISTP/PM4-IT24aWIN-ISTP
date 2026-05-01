"use client";

import React from "react";

interface BadgeSvgProps {
  color?: string;
  textColor?: string;
  template?: number;
  icon?: string;
  title?: string;
  size?: number;
}

function hexToRgb(hex: string): [number, number, number] {
  const clean = hex.replace("#", "");
  return [
    parseInt(clean.substring(0, 2), 16),
    parseInt(clean.substring(2, 4), 16),
    parseInt(clean.substring(4, 6), 16),
  ];
}

function lighten(hex: string, factor: number): string {
  const [r, g, b] = hexToRgb(hex);
  const nr = Math.min(255, Math.round(r + (255 - r) * factor));
  const ng = Math.min(255, Math.round(g + (255 - g) * factor));
  const nb = Math.min(255, Math.round(b + (255 - b) * factor));
  return `#${nr.toString(16).padStart(2, "0")}${ng.toString(16).padStart(2, "0")}${nb.toString(16).padStart(2, "0")}`;
}

function darken(hex: string, factor: number): string {
  const [r, g, b] = hexToRgb(hex);
  const nr = Math.max(0, Math.round(r * (1 - factor)));
  const ng = Math.max(0, Math.round(g * (1 - factor)));
  const nb = Math.max(0, Math.round(b * (1 - factor)));
  return `#${nr.toString(16).padStart(2, "0")}${ng.toString(16).padStart(2, "0")}${nb.toString(16).padStart(2, "0")}`;
}

function CircleBadge({ color, textColor, icon, title, size }: { color: string; textColor: string; icon: string; title: string; size: number }) {
  const light = lighten(color, 0.3);
  const dark = darken(color, 0.4);
  return (
    <svg viewBox="0 0 300 300" xmlns="http://www.w3.org/2000/svg" width={size} height={size}>
      <defs>
        <linearGradient id="cbg" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0%" stopColor={light} />
          <stop offset="100%" stopColor={dark} />
        </linearGradient>
        <radialGradient id="cfade" cx="50%" cy="72%" r="52%">
          <stop offset="0%" stopColor={dark} stopOpacity="0.85" />
          <stop offset="100%" stopColor={dark} stopOpacity="0" />
        </radialGradient>
        <clipPath id="cclip">
          <circle cx="150" cy="150" r="130" />
        </clipPath>
      </defs>
      <circle cx="150" cy="150" r="130" fill="url(#cbg)" />
      <circle cx="150" cy="150" r="120" fill="none" stroke={textColor} strokeWidth="2" strokeOpacity="0.25" />
      <circle cx="150" cy="150" r="108" fill="none" stroke={textColor} strokeWidth="1" strokeOpacity="0.15" />
      <rect x="20" y="195" width="260" height="90" fill="url(#cfade)" clipPath="url(#cclip)" />
      <text x="150" y="158" textAnchor="middle" fontSize="72" dominantBaseline="middle">{icon}</text>
      <text x="150" y="245" textAnchor="middle" fontSize="18" fill={textColor} fontWeight="bold" fontFamily="system-ui, sans-serif">{title}</text>
    </svg>
  );
}

function HexBadge({ color, textColor, icon, title, size }: { color: string; textColor: string; icon: string; title: string; size: number }) {
  const light = lighten(color, 0.3);
  const dark = darken(color, 0.4);
  const cx = 150, cy = 155, R = 135;
  const hexPoints = (r: number) =>
    Array.from({ length: 6 }, (_, i) => {
      const angle = (Math.PI / 180) * (60 * i - 30);
      return `${cx + r * Math.cos(angle)},${cy + r * Math.sin(angle)}`;
    }).join(" ");
  return (
    <svg viewBox="0 0 300 310" xmlns="http://www.w3.org/2000/svg" width={size} height={Math.round((size * 310) / 300)}>
      <defs>
        <linearGradient id="hbg" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0%" stopColor={light} />
          <stop offset="100%" stopColor={dark} />
        </linearGradient>
        <radialGradient id="hfade" cx="50%" cy="75%" r="50%">
          <stop offset="0%" stopColor={dark} stopOpacity="0.85" />
          <stop offset="100%" stopColor={dark} stopOpacity="0" />
        </radialGradient>
        <clipPath id="hclip">
          <polygon points={hexPoints(R)} />
        </clipPath>
      </defs>
      <polygon points={hexPoints(R)} fill="url(#hbg)" />
      <polygon points={hexPoints(R - 10)} fill="none" stroke={textColor} strokeWidth="2" strokeOpacity="0.25" />
      <polygon points={hexPoints(R - 22)} fill="none" stroke={textColor} strokeWidth="1" strokeOpacity="0.15" />
      <rect x="20" y="195" width="260" height="100" fill="url(#hfade)" clipPath="url(#hclip)" />
      <text x={cx} y={cy - 12} textAnchor="middle" fontSize="72" dominantBaseline="middle">{icon}</text>
      <text x={cx} y={cy + 72} textAnchor="middle" fontSize="18" fill={textColor} fontWeight="bold" fontFamily="system-ui, sans-serif">{title}</text>
    </svg>
  );
}

function MedalBadge({ color, textColor, icon, title, size }: { color: string; textColor: string; icon: string; title: string; size: number }) {
  const light = lighten(color, 0.3);
  const dark = darken(color, 0.4);
  const ribbonMid = lighten(color, 0.15);
  return (
    <svg viewBox="0 0 300 360" xmlns="http://www.w3.org/2000/svg" width={size} height={Math.round((size * 360) / 300)}>
      <defs>
        <linearGradient id="mbg" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0%" stopColor={light} />
          <stop offset="100%" stopColor={dark} />
        </linearGradient>
        <linearGradient id="mribbon" x1="0" y1="0" x2="1" y2="0">
          <stop offset="0%" stopColor={dark} />
          <stop offset="50%" stopColor={ribbonMid} />
          <stop offset="100%" stopColor={dark} />
        </linearGradient>
        <radialGradient id="mshine" cx="38%" cy="35%" r="50%">
          <stop offset="0%" stopColor="white" stopOpacity="0.22" />
          <stop offset="100%" stopColor="white" stopOpacity="0" />
        </radialGradient>
        <radialGradient id="mfade" cx="50%" cy="78%" r="45%">
          <stop offset="0%" stopColor={dark} stopOpacity="0.82" />
          <stop offset="100%" stopColor={dark} stopOpacity="0" />
        </radialGradient>
        <clipPath id="mclip">
          <circle cx="150" cy="215" r="120" />
        </clipPath>
      </defs>
      {/* Ribbon strips */}
      <polygon points="122,28 150,28 148,95 124,95" fill="url(#mribbon)" />
      <polygon points="150,28 178,28 176,95 152,95" fill="url(#mribbon)" />
      <line x1="150" y1="28" x2="150" y2="95" stroke={textColor} strokeWidth="0.5" strokeOpacity="0.2" />
      {/* Clasp bar */}
      <rect x="113" y="12" width="74" height="18" rx="5" fill={ribbonMid} stroke={textColor} strokeWidth="1" strokeOpacity="0.3" />
      <rect x="118" y="16" width="64" height="10" rx="3" fill="none" stroke={textColor} strokeWidth="0.5" strokeOpacity="0.2" />
      {/* Medal circle */}
      <circle cx="150" cy="215" r="120" fill="url(#mbg)" />
      <circle cx="150" cy="215" r="118" fill="none" stroke={textColor} strokeWidth="3" strokeDasharray="8 5" strokeOpacity="0.45" />
      <circle cx="150" cy="215" r="106" fill="none" stroke={textColor} strokeWidth="1.5" strokeOpacity="0.2" />
      <circle cx="150" cy="215" r="94" fill="none" stroke={textColor} strokeWidth="1" strokeOpacity="0.12" />
      <circle cx="150" cy="215" r="120" fill="url(#mshine)" />
      <rect x="20" y="258" width="260" height="80" fill="url(#mfade)" clipPath="url(#mclip)" />
      <text x="150" y="220" textAnchor="middle" fontSize="72" dominantBaseline="middle">{icon}</text>
      <text x="150" y="305" textAnchor="middle" fontSize="17" fill={textColor} fontWeight="bold" fontFamily="system-ui, sans-serif">{title}</text>
    </svg>
  );
}

export default function BadgeSvg({ color = "#4f46e5", textColor = "#ffffff", template = 1, icon = "🏆", title = "", size = 200 }: BadgeSvgProps) {
  const truncated = title.length > 20 ? title.slice(0, 20) + "…" : title;
  if (template === 2) return <HexBadge color={color} textColor={textColor} icon={icon} title={truncated} size={size} />;
  if (template === 3) return <MedalBadge color={color} textColor={textColor} icon={icon} title={truncated} size={size} />;
  return <CircleBadge color={color} textColor={textColor} icon={icon} title={truncated} size={size} />;
}
