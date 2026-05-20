export type FooterLink = { label: string; href: string };
export type FooterSection = { title: string; links: FooterLink[] };

export const FOOTER_SECTIONS: FooterSection[] = [
  {
    title: "Project",
    links: [
      { label: "GitHub", href: "https://github.com/PM4-ISTP/PM4-IT24aWIN-ISTP" },
      { label: "Issues", href: "https://github.com/PM4-ISTP/PM4-IT24aWIN-ISTP/issues" },
    ],
  },
];
