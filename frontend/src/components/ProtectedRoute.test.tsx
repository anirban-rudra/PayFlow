import { screen } from "@testing-library/react";
import { Route, Routes } from "react-router-dom";
import { describe, expect, it } from "vitest";
import { createTestToken } from "../test/jwt";
import { renderWithProviders } from "../test/render";
import { ProtectedRoute } from "./ProtectedRoute";

function TestRoutes({ requireAdmin = false }: { requireAdmin?: boolean }) {
  return (
    <Routes>
      <Route
        path="/protected"
        element={
          <ProtectedRoute requireAdmin={requireAdmin}>
            <div>Protected content</div>
          </ProtectedRoute>
        }
      />
      <Route path="/auth/login" element={<div>Login page</div>} />
      <Route path="/app/dashboard" element={<div>Dashboard page</div>} />
    </Routes>
  );
}

describe("ProtectedRoute", () => {
  it("redirects unauthenticated users to login", () => {
    renderWithProviders(<TestRoutes />, { route: "/protected" });
    expect(screen.getByText("Login page")).toBeInTheDocument();
  });

  it("allows authenticated app users", () => {
    sessionStorage.setItem("payflow.session", createTestToken());
    renderWithProviders(<TestRoutes />, { route: "/protected" });
    expect(screen.getByText("Protected content")).toBeInTheDocument();
  });

  it("redirects non-admin users away from admin routes", () => {
    sessionStorage.setItem("payflow.session", createTestToken({ role: "ROLE_USER" }));
    renderWithProviders(<TestRoutes requireAdmin />, { route: "/protected" });
    expect(screen.getByText("Dashboard page")).toBeInTheDocument();
  });

  it("allows admin users through admin routes", () => {
    sessionStorage.setItem("payflow.session", createTestToken({ role: "ROLE_ADMIN" }));
    renderWithProviders(<TestRoutes requireAdmin />, { route: "/protected" });
    expect(screen.getByText("Protected content")).toBeInTheDocument();
  });
});
