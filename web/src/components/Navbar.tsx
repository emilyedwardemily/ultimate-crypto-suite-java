"use client";

import { useRouter } from "next/navigation";
import { Menu, Bell, LogOut } from "lucide-react";
import { useAuth } from "@/lib/auth";

export default function Navbar({
  onMenuClick,
}: {
  onMenuClick: () => void;
}) {
  const router = useRouter();
  const { user, logout } = useAuth();

  const handleLogout = () => {
    logout();
    router.push("/login");
  };

  const initials = user?.name
    ? user.name
        .split(" ")
        .map((n) => n[0])
        .join("")
        .toUpperCase()
        .slice(0, 2)
    : "?";

  return (
    <header className="flex h-16 items-center justify-between border-b border-crypto-border bg-crypto-surface px-6">
      <button
        onClick={onMenuClick}
        className="rounded-lg p-2 text-crypto-text-muted hover:bg-crypto-surface2 lg:hidden"
      >
        <Menu size={20} />
      </button>
      <div className="flex-1" />
      <div className="flex items-center gap-4">
        <button
          onClick={() => router.push("/audit-logs")}
          className="relative rounded-lg p-2 text-crypto-text-muted hover:bg-crypto-surface2"
          title="View audit logs"
        >
          <Bell size={20} />
          <span className="absolute right-1.5 top-1.5 h-2 w-2 rounded-full bg-crypto-red" />
        </button>
        <div className="flex items-center gap-3">
          <div className="flex h-8 w-8 items-center justify-center rounded-full bg-crypto-accent/20 text-sm font-semibold text-crypto-accent">
            {initials}
          </div>
          <span className="hidden text-sm font-medium text-crypto-text md:inline">
            {user?.name || "User"}
          </span>
        </div>
        <button
          onClick={handleLogout}
          className="rounded-lg p-2 text-crypto-text-muted hover:bg-crypto-surface2"
        >
          <LogOut size={20} />
        </button>
      </div>
    </header>
  );
}
