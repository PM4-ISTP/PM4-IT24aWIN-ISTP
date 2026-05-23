import { getServerSession } from "next-auth";
import { authOptions } from "@/src/shared/lib/auth";
import DashboardShell from "@/src/shared/components/DashboardShell";

export default async function DashboardLayout({ children }: { children: React.ReactNode }) {
  const session = await getServerSession(authOptions);
  const name = session?.user?.name ?? "Unknown";
  const image = session?.user?.image ?? null;
  const roles = (session?.roles as string[]) ?? [];

  return (
    <DashboardShell name={name} roles={roles} image={image} userId={session?.userId ?? null}>
      {children}
    </DashboardShell>
  );
}
