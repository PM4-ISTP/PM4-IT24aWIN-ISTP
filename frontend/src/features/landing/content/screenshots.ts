export type Screenshot = {
  src: string;
  title: string;
  role: string;
  description: string;
  url: string;
};

export const SCREENSHOTS: Screenshot[] = [
  {
    src: "/images/landing/Home.png",
    title: "Home dashboard",
    role: "student",
    description:
      "Enrolled courses, completed labs and time online — all your progress at a glance.",
    url: "istp.pm4.init-lab.ch",
  },
  {
    src: "/images/landing/Course_catalog.png",
    title: "Browse catalog",
    role: "everyone",
    description: "Search and filter every course running on this ISTP instance.",
    url: "istp.pm4.init-lab.ch/catalog",
  },
  {
    src: "/images/landing/Course_overview.png",
    title: "Course overview",
    role: "student",
    description: "All labs in a course, your per-lab progress, and what's due next — in one view.",
    url: "istp.pm4.init-lab.ch/courses/web-security",
  },
  {
    src: "/images/landing/Course_lab.png",
    title: "Lab · live pod",
    role: "student",
    description:
      "Challenge brief, a Kubernetes pod running just for you, and a flag submission box.",
    url: "istp.pm4.init-lab.ch/lab/campus-helpdesk",
  },
];
