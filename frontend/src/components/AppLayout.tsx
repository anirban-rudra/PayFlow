import {
  Bell,
  CircleDollarSign,
  CreditCard,
  Landmark,
  Gift,
  History,
  LayoutDashboard,
  LogOut,
  Menu,
  Send,
  Shield,
  User,
  Users,
  X,
  type LucideIcon
} from "lucide-react";
import { useState } from "react";
import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { useCurrentUser } from "../hooks/usePayflow";
import { useAuth } from "../state/auth";

const primaryNav = [
  { to: "/app/dashboard", label: "Dashboard", icon: LayoutDashboard },
  { to: "/app/top-up", label: "Top Up Wallet", icon: Landmark },
  { to: "/app/send", label: "Send", icon: Send },
  { to: "/app/transactions", label: "History", icon: History },
  { to: "/app/rewards", label: "Rewards", icon: Gift },
  { to: "/app/notifications", label: "Alerts", icon: Bell }
];

const mobileNav = [
  { to: "/app/dashboard", label: "Dashboard", icon: LayoutDashboard },
  { to: "/app/send", label: "Send", icon: Send },
  { to: "/app/transactions", label: "History", icon: History },
  { to: "/app/notifications", label: "Alerts", icon: Bell }
];

const adminNav = [
  { to: "/admin/users", label: "Users", icon: Users },
  { to: "/admin/rewards", label: "Reward Ops", icon: CircleDollarSign },
  { to: "/admin/notifications", label: "Notify Ops", icon: Shield }
];

export function AppLayout() {
  const [open, setOpen] = useState(false);
  const [desktopCollapsed, setDesktopCollapsed] = useState(false);
  const { user, logout } = useAuth();
  const currentUser = useCurrentUser(user?.userId);
  const navigate = useNavigate();
  const displayName = currentUser.data?.name ?? "Account";

  const signOut = () => {
    logout();
    navigate("/auth/login", { replace: true });
  };

  const openNavigation = () => {
    setDesktopCollapsed(false);
    setOpen(true);
  };

  const closeNavigation = () => {
    setOpen(false);
    setDesktopCollapsed(true);
  };

  return (
    <div className={`app-shell ${desktopCollapsed ? "sidebar-collapsed" : ""}`}>
      <aside className={`sidebar ${open ? "sidebar-open" : ""}`}>
        <div className="brand-row">
          <div className="brand-mark">P</div>
          <div>
            <div className="brand-name">PayFlow</div>
            <div className="brand-subtitle">Payments</div>
          </div>
          <button className="icon-button sidebar-close" type="button" onClick={closeNavigation} aria-label="Close navigation">
            <X size={18} />
          </button>
        </div>

        <div className="sidebar-scroll">
          <nav className="side-nav">
            {primaryNav.map((item) => (
              <NavItem key={item.to} {...item} onClick={() => setOpen(false)} />
            ))}
          </nav>

          {user?.role === "ROLE_ADMIN" && (
            <nav className="side-nav admin-nav">
              <div className="nav-section">Admin</div>
              {adminNav.map((item) => (
                <NavItem key={item.to} {...item} onClick={() => setOpen(false)} />
              ))}
            </nav>
          )}
        </div>

        <div className="sidebar-footer">
          <NavLink className="nav-item" to="/app/profile" onClick={() => setOpen(false)}>
            <User size={18} />
            <span>Profile</span>
          </NavLink>
          <button className="nav-item nav-action" type="button" onClick={signOut}>
            <LogOut size={18} />
            <span>Logout</span>
          </button>
        </div>
      </aside>

      {open && <button className="scrim" type="button" aria-label="Close navigation" onClick={() => setOpen(false)} />}

      <div className="main-shell">
        <header className="topbar">
          <button className="icon-button mobile-menu" type="button" onClick={openNavigation} aria-label="Open navigation">
            <Menu size={20} />
          </button>
          <div className="topbar-title">
            <span>PayFlow</span>
            <small>{displayName}</small>
          </div>
        </header>

        <main className="content-shell">
          <Outlet />
        </main>
      </div>

      <nav className="bottom-nav">
        {mobileNav.map((item) => (
          <NavLink key={item.to} to={item.to} className="bottom-nav-item">
            <item.icon size={19} />
            <span>{item.label}</span>
          </NavLink>
        ))}
      </nav>
    </div>
  );
}

function NavItem({
  to,
  label,
  icon: Icon,
  onClick
}: {
  to: string;
  label: string;
  icon: LucideIcon;
  onClick?: () => void;
}) {
  return (
    <NavLink className="nav-item" to={to} onClick={onClick}>
      <Icon size={18} />
      <span>{label}</span>
    </NavLink>
  );
}
