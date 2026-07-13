import { Card, EmptyState, ErrorBanner, PageHeader } from "../components/ui";
import { useAdminUsers } from "../hooks/usePayflow";
import { useAuth } from "../state/auth";

export function AdminUsersPage() {
  const { user } = useAuth();
  const users = useAdminUsers(user?.role === "ROLE_ADMIN");

  return (
    <div className="page-stack">
      <PageHeader title="Users" />
      <ErrorBanner message={users.error ? "Could not load users." : undefined} />
      <Card>
        <div className="desktop-table">
          <table>
            <thead>
              <tr>
                <th>ID</th>
                <th>Name</th>
                <th>Email</th>
                <th>Role</th>
              </tr>
            </thead>
            <tbody>
              {(users.data ?? []).map((record) => (
                <tr key={record.id}>
                  <td>#{record.id}</td>
                  <td>{record.name}</td>
                  <td>{record.email}</td>
                  <td>{record.role === "ROLE_ADMIN" ? "Admin" : "User"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <div className="mobile-list">
          {(users.data ?? []).map((record) => (
            <div className="mobile-record" key={record.id}>
              <div>
                <strong>{record.name}</strong>
                <span>#{record.id}</span>
              </div>
              <div>
                <span>{record.email}</span>
                <strong>{record.role === "ROLE_ADMIN" ? "Admin" : "User"}</strong>
              </div>
            </div>
          ))}
        </div>
        {!users.isLoading && (users.data ?? []).length === 0 && <EmptyState title="No users found" />}
      </Card>
    </div>
  );
}
