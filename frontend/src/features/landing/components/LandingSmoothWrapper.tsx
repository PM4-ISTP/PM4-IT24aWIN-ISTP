"use client";

import { useRef } from "react";
import { useGSAP } from "@gsap/react";
import { gsap } from "gsap";
import { ScrollTrigger } from "gsap/ScrollTrigger";
import { ScrollSmoother } from "gsap/ScrollSmoother";

gsap.registerPlugin(ScrollTrigger, ScrollSmoother);

ScrollTrigger.config({ ignoreMobileResize: true });

type LandingSmoothWrapperProps = {
  children: React.ReactNode;
};

export default function LandingSmoothWrapper({ children }: LandingSmoothWrapperProps) {
  const wrapperRef = useRef<HTMLDivElement>(null);
  const contentRef = useRef<HTMLDivElement>(null);

  useGSAP(
    () => {
      const wrapper = wrapperRef.current;
      const content = contentRef.current;
      if (!wrapper || !content) return;

      const prefersReducedMotion =
        typeof window !== "undefined" &&
        window.matchMedia("(prefers-reduced-motion: reduce)").matches;

      const isTouch =
        typeof window !== "undefined" &&
        (window.matchMedia("(pointer: coarse)").matches || "ontouchstart" in window);

      const smoother =
        prefersReducedMotion || isTouch
          ? null
          : ScrollSmoother.create({
              wrapper,
              content,
              smooth: 1.1,
              smoothTouch: 0,
            });

      const handleAnchorClick = (event: MouseEvent) => {
        const target = event.target as HTMLElement | null;
        const anchor = target?.closest("a[href^='#']") as HTMLAnchorElement | null;
        if (!anchor) return;
        const hash = anchor.getAttribute("href");
        if (!hash || hash === "#") return;
        let elementId = hash.slice(1);
        try {
          elementId = decodeURIComponent(elementId);
        } catch {
          // keep raw id if not valid percent-encoding
        }
        const element = document.getElementById(elementId);
        if (!element) return;
        event.preventDefault();
        if (smoother) {
          smoother.scrollTo(element, true, "top top");
        } else {
          element.scrollIntoView({ behavior: "auto", block: "start" });
        }
      };

      document.addEventListener("click", handleAnchorClick);

      return () => {
        document.removeEventListener("click", handleAnchorClick);
        smoother?.kill();
        ScrollTrigger.refresh();
      };
    },
    { scope: wrapperRef }
  );

  return (
    <div id="smooth-wrapper" ref={wrapperRef}>
      <div id="smooth-content" ref={contentRef}>
        {children}
      </div>
    </div>
  );
}
