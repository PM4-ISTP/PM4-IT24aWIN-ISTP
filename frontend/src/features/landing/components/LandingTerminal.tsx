"use client";

import { Box, Container, Stack, Text } from "@mantine/core";
import { useRef } from "react";
import type { CSSProperties } from "react";
import { useGSAP } from "@gsap/react";
import { gsap } from "gsap";
import { ScrollTrigger } from "gsap/ScrollTrigger";
import HeroTerminal from "./HeroTerminal";
import { addDesktopMotion, addMobileMotion, addReducedMotion } from "../hooks/useScrollAnimations";
import { TERMINAL_BUBBLES } from "../content/terminalBubbles";
import { INK, INK_DIM, LINE_2 } from "../theme";

gsap.registerPlugin(ScrollTrigger);

type BubbleCalloutProps = {
  title: string;
  body: string;
  style: CSSProperties;
  setRef: (el: HTMLDivElement | null) => void;
  className?: string;
};

function BubbleCallout({ title, body, style, setRef, className }: BubbleCalloutProps) {
  return (
    <Box
      ref={setRef}
      className={className}
      style={{
        position: "absolute",
        zIndex: 3,
        maxWidth: 260,
        padding: "12px 14px",
        borderRadius: 14,
        border: `1px solid ${LINE_2}`,
        background: "rgba(12,16,28,0.2)",
        color: INK,
        boxShadow: "0 20px 50px -25px rgba(0,0,0,0.6)",
        backdropFilter: "blur(8px)",
        ...style,
      }}
    >
      <Text style={{ fontSize: 12.5, color: INK_DIM, marginBottom: 6 }}>{title}</Text>
      <Text style={{ fontSize: 13.5, lineHeight: 1.5 }}>{body}</Text>
    </Box>
  );
}

export default function LandingTerminal() {
  const sectionRef = useRef<HTMLElement>(null);
  const pinRef = useRef<HTMLDivElement>(null);
  const terminalRef = useRef<HTMLDivElement>(null);
  const bubbleRefs = useRef<Array<HTMLDivElement | null>>([]);

  useGSAP(
    () => {
      const section = sectionRef.current;
      const pin = pinRef.current;
      const terminal = terminalRef.current;
      const bubbles = bubbleRefs.current.filter(Boolean) as HTMLDivElement[];
      if (!section || !pin || !terminal) return;

      const mm = gsap.matchMedia();

      addReducedMotion(mm, () => {
        gsap.set(terminal, { scale: 1, transformOrigin: "center top" });
        gsap.set(bubbles, { opacity: 1, y: 0, scale: 1 });
      });

      addDesktopMotion(mm, () => {
        gsap.set(terminal, { scale: 0.96, transformOrigin: "center top" });
        gsap.set(bubbles, { opacity: 0, y: 18, scale: 0.98 });

        const tl = gsap.timeline({
          defaults: { ease: "power2.out" },
          scrollTrigger: {
            trigger: section,
            start: "top 10%",
            end: "bottom top",
            scrub: 1,
            pin,
            pinSpacing: true,
            anticipatePin: 1,
            invalidateOnRefresh: true,
          },
        });

        tl.to(terminal, { scale: 1, duration: 0.6 });
        if (bubbles.length) {
          tl.to(bubbles, { opacity: 1, y: 0, scale: 1, duration: 0.25, stagger: 0.2 }, 0.35);
          tl.to(
            bubbles,
            {
              opacity: 0,
              y: 12,
              scale: 0.98,
              duration: 0.22,
              stagger: { each: 0.18, from: "end" },
            },
            1.45
          );
        }
        tl.to(terminal, { scale: 0.96, duration: 0.6 }, 1.7);
      });

      addMobileMotion(mm, () => {
        gsap.set(terminal, { scale: 1, transformOrigin: "center top" });
        gsap.fromTo(
          terminal,
          { opacity: 0, y: 30 },
          {
            opacity: 1,
            y: 0,
            duration: 0.7,
            ease: "power2.out",
            scrollTrigger: { trigger: section, start: "top 85%", once: true },
          }
        );
        if (bubbles.length) {
          gsap.fromTo(
            bubbles,
            { opacity: 0, y: 18, scale: 0.98 },
            {
              opacity: 1,
              y: 0,
              scale: 1,
              duration: 0.5,
              stagger: 0.12,
              ease: "power2.out",
              scrollTrigger: { trigger: section, start: "top 75%", once: true },
            }
          );
        }
      });
    },
    { scope: sectionRef }
  );

  return (
    <Box
      component="section"
      ref={sectionRef}
      className="landing-terminal-section"
      style={{ padding: "0 0 120px", minHeight: "120svh" }}
    >
      <Container size="xl" px={32}>
        <Stack gap={24}>
          <Box
            ref={pinRef}
            style={{
              display: "flex",
              alignItems: "flex-start",
              justifyContent: "center",
              paddingTop: 0,
            }}
          >
            <Box
              ref={terminalRef}
              style={{
                width: "100%",
                position: "relative",
              }}
            >
              <HeroTerminal />

              {TERMINAL_BUBBLES.map((bubble, index) => (
                <BubbleCallout
                  key={bubble.title}
                  title={bubble.title}
                  body={bubble.body}
                  style={bubble.style}
                  className={`bubble-callout bubble-${bubble.title.toLowerCase()}`}
                  setRef={(el) => {
                    bubbleRefs.current[index] = el;
                  }}
                />
              ))}
            </Box>
          </Box>
        </Stack>
      </Container>

      <style>{`
        @media (max-width: 900px) {
          .landing-terminal-section { min-height: 0 !important; padding-bottom: 60px !important; }
          .bubble-callout { display: none; }
          .bubble-overview { display: block; }
        }
      `}</style>
    </Box>
  );
}
