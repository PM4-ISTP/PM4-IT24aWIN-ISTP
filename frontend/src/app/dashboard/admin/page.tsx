import { getServerSession } from "next-auth";
import { authOptions } from "@/src/lib/auth";
import { redirect } from "next/navigation";

export default async function AdminDashboard() {
  const session = await getServerSession(authOptions);

  // Role guard — only ROLE_ADMIN can access this page
  const roles: string[] = (session?.roles as string[]) ?? [];
  if (!roles.includes("ROLE_ADMINISTRATOR")) {
    redirect("/dashboard");
  }

  return (
    <div className="p-8">
      <h1 className="text-2xl font-bold mb-6">Admin Dashboard</h1>

      <div className="grid gap-4">
        <a
          href="http://localhost:9090/admin"
          target="_blank"
          rel="noopener noreferrer"
          className="flex items-center gap-3 p-4 border rounded-lg hover:bg-gray-50 transition"
        >
          <div>
            <h2 className="font-semibold">Manage Users</h2>
            <p className="text-sm text-gray-500">
              Edit or delete users via Keycloak Admin Console
            </p>
          </div>
          <span className="ml-auto">→</span>
        </a>

        <a
          href="http://localhost:9090/admin"
          target="_blank"
          rel="noopener noreferrer"
          className="flex items-center gap-3 p-4 border rounded-lg hover:bg-gray-50 transition"
        >
          <div>
            <h2 className="font-semibold">Manage Roles</h2>
            <p className="text-sm text-gray-500">
              Assign or remove roles via Keycloak Admin Console
            </p>
          </div>
          <span className="ml-auto">→</span>
        </a>
      </div>
    </div>
  );
}
