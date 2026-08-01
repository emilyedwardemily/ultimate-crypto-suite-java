"use client";

import { useEffect, useState } from "react";
import { Trophy, Medal, TrendingUp, Flame } from "lucide-react";
import { getLeaderboard } from "@/lib/api";

interface LeaderboardEntry {
  rank: number;
  username: string;
  xp: number;
  level: number;
  badges: number;
  challengesSolved: number;
}

const rankIcons: Record<number, typeof Trophy> = {
  1: Trophy,
  2: Medal,
  3: Medal,
};

const rankColors: Record<number, string> = {
  1: "text-crypto-yellow",
  2: "text-crypto-text-muted",
  3: "text-amber-600",
};

function xpToPercent(xp: number, maxXp: number) {
  return maxXp > 0 ? Math.min((xp / maxXp) * 100, 100) : 0;
}

export default function LeaderboardPage() {
  const [entries, setEntries] = useState<LeaderboardEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    getLeaderboard()
      .then((res) => setEntries(res.data.entries || res.data))
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, []);

  const maxXp =
    entries.length > 0 ? Math.max(...entries.map((e) => e.xp)) : 0;

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Leaderboard</h1>
        <p className="mt-1 text-sm text-crypto-text-muted">
          Top cryptographers ranked by XP and achievements.
        </p>
      </div>

      {error && (
        <div className="rounded-lg border border-crypto-red/30 bg-crypto-red/10 p-4 text-sm text-crypto-red">
          {error}
        </div>
      )}

      {loading ? (
        <div className="space-y-3">
          {Array.from({ length: 5 }).map((_, i) => (
            <div
              key={i}
              className="card flex animate-pulse items-center gap-4"
            >
              <div className="h-8 w-8 rounded-full bg-crypto-surface2" />
              <div className="flex-1 space-y-2">
                <div className="h-4 w-1/4 rounded bg-crypto-surface2" />
                <div className="h-2 w-full rounded bg-crypto-surface2" />
              </div>
            </div>
          ))}
        </div>
      ) : (
        <div className="space-y-3">
          {entries.map((entry) => {
            const Icon = rankIcons[entry.rank] || TrendingUp;
            const color = rankColors[entry.rank] || "text-crypto-text-muted";
            const barPercent = xpToPercent(entry.xp, maxXp);

            return (
              <div key={entry.rank} className="card flex items-center gap-4">
                <div className={`flex h-10 w-10 items-center justify-center ${color}`}>
                  {entry.rank <= 3 ? (
                    <Icon className="h-6 w-6" />
                  ) : (
                    <span className="text-lg font-bold text-crypto-text-muted">
                      {entry.rank}
                    </span>
                  )}
                </div>
                <div className="flex-1 space-y-2">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2">
                      <span className="font-semibold">{entry.username}</span>
                      <span className="rounded bg-crypto-accent/10 px-2 py-0.5 text-xs font-medium text-crypto-accent">
                        Lv.{entry.level}
                      </span>
                    </div>
                    <span className="text-sm font-medium text-crypto-yellow">
                      {entry.xp.toLocaleString()} XP
                    </span>
                  </div>
                  <div className="h-2 w-full overflow-hidden rounded-full bg-crypto-surface2">
                    <div
                      className="h-full rounded-full bg-gradient-to-r from-crypto-accent to-crypto-blue transition-all duration-500"
                      style={{ width: `${barPercent}%` }}
                    />
                  </div>
                  <div className="flex gap-4 text-xs text-crypto-text-muted">
                    <span className="flex items-center gap-1">
                      <Flame className="h-3 w-3" />
                      {entry.challengesSolved} solved
                    </span>
                    <span>{entry.badges} badges</span>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
