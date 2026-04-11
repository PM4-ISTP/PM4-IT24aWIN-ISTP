"use client";
import { useState } from "react";

export interface TooltipItem {
  id: number;
  name: string;
  designation: string;
  image: string;
}

export function AnimatedTooltip({ items }: { items: TooltipItem[] }) {
  const [hoveredId, setHoveredId] = useState<number | null>(null);

  return (
    <div style={{ display: "flex", flexDirection: "row" }}>
      {items.map((item, idx) => (
        <div
          key={item.id}
          style={{ position: "relative", marginLeft: idx === 0 ? 0 : -12 }}
          tabIndex={0}
          onMouseEnter={() => setHoveredId(item.id)}
          onMouseLeave={() => setHoveredId(null)}
          onFocus={() => setHoveredId(item.id)}
          onBlur={() => setHoveredId(null)}
          aria-describedby={hoveredId === item.id ? `tooltip-${item.id}` : undefined}
        >
          {/* Tooltip */}
          <div
            id={`tooltip-${item.id}`}
            role="tooltip"
            aria-hidden={hoveredId !== item.id}
            style={{
              position: "absolute",
              bottom: "calc(100% + 10px)",
              left: "50%",
              transform: `translateX(-50%) ${hoveredId === item.id ? "translateY(0) scale(1)" : "translateY(6px) scale(0.92)"}`,
              opacity: hoveredId === item.id ? 1 : 0,
              pointerEvents: "none",
              transition: "all 0.2s cubic-bezier(0.34,1.56,0.64,1)",
              zIndex: 50,
              whiteSpace: "nowrap",
            }}
          >
            {/* Arrow */}
            <div
              style={{
                position: "absolute",
                top: "100%",
                left: "50%",
                transform: "translateX(-50%)",
                width: 0,
                height: 0,
                borderLeft: "5px solid transparent",
                borderRight: "5px solid transparent",
                borderTop: "5px solid #1e293b",
              }}
            />
            <div
              style={{
                background: "linear-gradient(135deg, #1e293b 0%, #0f172a 100%)",
                border: "1px solid rgba(96,165,250,0.2)",
                borderRadius: 8,
                padding: "6px 12px",
                boxShadow: "0 8px 24px rgba(0,0,0,0.5)",
              }}
            >
              <p
                style={{
                  color: "#f1f5f9",
                  fontWeight: 700,
                  fontSize: "0.82rem",
                  margin: 0,
                  fontFamily: "var(--font-space-grotesk), sans-serif",
                }}
              >
                {item.name}
              </p>
              <p
                style={{
                  color: "#60a5fa",
                  fontSize: "0.72rem",
                  margin: 0,
                  fontFamily: "var(--font-space-grotesk), sans-serif",
                }}
              >
                {item.designation}
              </p>
            </div>
          </div>

          {/* Avatar */}
          <div
            style={{
              width: 44,
              height: 44,
              borderRadius: "50%",
              overflow: "hidden",
              border:
                hoveredId === item.id ? "2px solid #60a5fa" : "2px solid rgba(255,255,255,0.15)",
              transform: hoveredId === item.id ? "scale(1.12) translateY(-3px)" : "scale(1)",
              transition: "all 0.2s cubic-bezier(0.34,1.56,0.64,1)",
              cursor: "pointer",
              position: "relative",
              zIndex: hoveredId === item.id ? 10 : idx,
              background: "rgba(96,165,250,0.08)",
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              boxShadow: hoveredId === item.id ? "0 0 0 3px rgba(96,165,250,0.25)" : "none",
            }}
          >
            {item.image ? (
              // eslint-disable-next-line @next/next/no-img-element
              <img
                src={item.image}
                alt={item.name}
                style={{ width: "100%", height: "100%", objectFit: "cover" }}
              />
            ) : (
              <span
                style={{
                  color: "#60a5fa",
                  fontWeight: 700,
                  fontSize: "0.9rem",
                  fontFamily: "var(--font-space-grotesk), sans-serif",
                  userSelect: "none",
                }}
              >
                {item.name
                  .split(" ")
                  .map((n) => n[0])
                  .join("")
                  .toUpperCase()
                  .slice(0, 2)}
              </span>
            )}
          </div>
        </div>
      ))}
    </div>
  );
}
