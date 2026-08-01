"use client";

import { useEffect, useState } from "react";
import {
  Users,
  Activity,
  Swords,
  TrendingUp,
  Shield,
  Server,
} from "lucide-react";
import { getDashboardStats } from "@/lib/api";

interface DashboardStats {
  totalUsers: number;
  activeSessions: number;
  ctfChallengesSolved: number;
  totalEncryptions: number;
  uptime: string;
  activeSubscriptions: number;
}

const defaultStats: DashboardStats = {
  totalUsers: 0,
  activeSessions: 0,
  ctfChallengesSolved: 0,
  totalEncryptions: 0,
  uptime: "99.9%",
  activeSubscriptions: 0,
};

const statCards = [
  { label: "Total Users", key: "totalUsers" as const, icon: Users, color: "text-crypto-blue" },
  { label: "Active Sessions", key: "activeSessions" as const, icon: Activity, color: "text-crypto-accent" },
  { label: "CTF Challenges Solved", key: "ctfChallengesSolved" as const, icon: Swords, color: "text-crypto-yellow" },
  { label: "Total Encryptions", key: "totalEncryptions" as const, icon: Shield, color: "text-crypto-purple" },
  { label: "Uptime", key: "uptime" as const, icon: Server, color: "text-crypto-accent" },
  { label: "Active Subscriptions", key: "activeSubscriptions" as const, icon: TrendingUp, color: "text-crypto-blue" },
];

export default function Dashboard() {
  const [stats, setStats] = useState<DashboardStats>(defaultStats);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    getDashboardStats()
      .then((res) => setStats(res.data))
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, []);

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Dashboard</h1>
        <p className="mt-1 text-sm text-crypto-text-muted">
          Real-time overview of the Ultimate Crypto Suite platform.
        </p>
      </div>

      {error && (
        <div className="rounded-lg border border-crypto-red/30 bg-crypto-red/10 p-4 text-sm text-crypto-red">
          Failed to load stats: {error}
        </div>
      )}

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {statCards.map((card) => {
          const Icon = card.icon;
          const value = stats[card.key];
          return (
            <div key={card.key} className="stat-card">
              <div className="flex items-center justify-between">
                <p className="text-sm text-crypto-text-muted">{card.label}</p>
                <Icon className={`h-5 w-5 ${card.color}`} />
              </div>
              <p className="mt-2 text-2xl font-bold">
                {loading ? (
                  <span className="inline-block h-6 w-16 animate-pulse rounded bg-crypto-surface2" />
                ) : (
                  value
                )}
              </p>
            </div>
          );
        })}
      </div>
    </div>
  );
}
