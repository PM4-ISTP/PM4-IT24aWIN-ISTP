"use client";

import type { CSSProperties, HTMLAttributes } from "react";
import { getSanitizedHtml } from "@/src/shared/lib/utils";

export interface SanitizedHtmlProps extends Omit<HTMLAttributes<HTMLDivElement>, "children"> {
  html: string;
  as?: "div" | "span" | "section" | "article";
  className?: string;
  style?: CSSProperties;
}

export function SanitizedHtml({
  html,
  as: Tag = "div",
  className,
  style,
  ...rest
}: SanitizedHtmlProps) {
  return (
    <Tag
      className={className}
      style={style}
      dangerouslySetInnerHTML={{ __html: getSanitizedHtml(html) }}
      {...rest}
    />
  );
}
