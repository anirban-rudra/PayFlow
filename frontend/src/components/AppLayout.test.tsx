import { screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { Route, Routes } from "react-router-dom";
import { describe, expect, it } from "vitest";
import { createTestToken } from "../test/jwt";
import { renderWithProviders } from "../test/render";
import { AppLayout } from "./AppLayout";

function LayoutRoutes() {
  return (
    <Routes>
      <Route path="/app" element={<AppLayout />}>
        <Route path="dashboard" element={<div>Dashboard content</div>} />
      </Route>
      <Route path="/auth/login" element={<div>Login page</div>} />
    </Routes>
  );
}

describe("AppLayout", () => {
  it("collapses and reopens the navigation shell", async () => {
    const user = userEvent.setup();
    sessionStorage.setItem("payflow.session", createTestToken());

    renderWithProviders(<LayoutRoutes />, { route: "/app/dashboard" });

    const shell = document.querySelector(".app-shell");
    expect(shell).not.toHaveClass("sidebar-collapsed");

    await user.click(screen.getByRole("button", { name: /close navigation/i }));
    expect(shell).toHaveClass("sidebar-collapsed");

    await user.click(screen.getByRole("button", { name: /open navigation/i }));
    expect(shell).not.toHaveClass("sidebar-collapsed");
  });

  it("keeps logout in a single navigation location", () => {
    sessionStorage.setItem("payflow.session", createTestToken());

    renderWithProviders(<LayoutRoutes />, { route: "/app/dashboard" });

    expect(screen.getAllByRole("button", { name: /logout/i })).toHaveLength(1);
  });

  it("places Top Up Wallet above Send in the desktop navigation", () => {
    sessionStorage.setItem("payflow.session", createTestToken());

    renderWithProviders(<LayoutRoutes />, { route: "/app/dashboard" });

    const sidebarNav = screen.getAllByRole("navigation")[0];
    const topUp = within(sidebarNav).getByRole("link", { name: /top up wallet/i });
    const send = within(sidebarNav).getByRole("link", { name: /^send$/i });

    expect(topUp.compareDocumentPosition(send) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
  });
});
