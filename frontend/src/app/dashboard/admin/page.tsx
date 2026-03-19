export const dynamic = "force-dynamic";

export default function AdminDashboard() {
  const keycloakAdminUrl =
    process.env.KEYCLOAK_ADMIN_URL ?? "http://localhost:9090/admin/";

  return (
    <div>
      <h1>Admin Dashboard</h1>
      <section>
        <h2>User Management</h2>
        <p>
          Manage users, roles, and permissions via the Keycloak Admin Console.
        </p>
        <a href={keycloakAdminUrl} target="_blank" rel="noopener noreferrer">
          Open Keycloak Admin Console
        </a>
      </section>
    </div>
  );
}
