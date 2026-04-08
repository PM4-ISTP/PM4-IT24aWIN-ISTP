import { Avatar } from "@mantine/core";

function getInitials(name: string): string {
  const parts = name.trim().split(/\s+/).filter(Boolean);
  if (parts.length === 0) {
    return "?";
  }

  return parts
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase() ?? "")
    .join("");
}

export default function UserAvatar({
  pictureUrl,
  userName,
}: {
  pictureUrl: string | null | undefined;
  userName: string;
}) {
  return (
    <Avatar radius="xl" color="blue" src={pictureUrl ?? undefined}>
      {getInitials(userName)}
    </Avatar>
  );
}
