"use client";

import { useEffect, useState } from "react";
import { Shield, Star, Award, Zap } from "lucide-react";
import { getProfile } from "@/lib/api";

const OPERATOR_ID = "UC-PRO-71468B1B";

interface Badge {
  id: string;
  name: string;
  icon: string;
  unlocked: boolean;
}

interface ProfileData {
  username: string;
  email: string;
  rank: number;
  level: number;
  xp: number;
  xpToNextLevel: number;
  badges: Badge[];
  joinDate: string;
}

const defaultProfile: ProfileData = {
  username: "\u2014",
  email: "\u2014",
  rank: 0,
  level: 1,
  xp: 0,
  xpToNextLevel: 1000,
  badges: [],
  joinDate: "\u2014",
};

const badgeIcons: Record<string, typeof Shield> = {
  shield: Shield,
  star: Star,
  award: Award,
  zap: Zap,
};

export default function ProfilePage() {
  const [profile, setProfile] = useState<ProfileData>(defaultProfile);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    getProfile(OPERATOR_ID)
      .then((res) => setProfile(res.data))
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, []);

  const xpPercent =
    profile.xpToNextLevel > 0
      ? Math.min((profile.xp / profile.xpToNextLevel) * 100, 100)
      : 0;

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Profile</h1>
        <p className="mt-1 text-sm text-crypto-text-muted">
          Your cryptographic journey at a glance.
        </p>
      </div>

      {error && (
        <div className="rounded-lg border border-crypto-red/30 bg-crypto-red/10 p-4 text-sm text-crypto-red">
          {error}
        </div>
      )}

      {loading ? (
        <div className="card animate-pulse space-y-4">
          <div className="h-16 w-16 rounded-full bg-crypto-surface2" />
          <div className="h-5 w-1/3 rounded bg-crypto-surface2" />
          <div className="h-3 w-1/2 rounded bg-crypto-surface2" />
        </div>
      ) : (
        <>
          <div className="card">
            <div className="flex flex-col items-center text-center sm:flex-row sm:gap-6 sm:text-left">
              <div className="flex h-20 w-20 items-center justify-center rounded-full bg-gradient-to-br from-crypto-accent to-crypto-blue text-2xl font-bold text-white">
                {profile.username.charAt(0).toUpperCase()}
              </div>
              <div className="mt-4 flex-1 sm:mt-0">
                <h2 className="text-xl font-bold">{profile.username}</h2>
                <p className="text-sm text-crypto-text-muted">
                  {profile.email}
                </p>
                <div className="mt-3 flex flex-wrap gap-3 text-sm">
                  <span className="rounded-lg border border-crypto-accent/30 bg-crypto-accent/10 px-3 py-1 font-medium text-crypto-accent">
                    Rank #{profile.rank}
                  </span>
                  <span className="rounded-lg border border-crypto-blue/30 bg-crypto-blue/10 px-3 py-1 font-medium text-crypto-blue">
                    Level {profile.level}
                  </span>
                  <span className="rounded-lg border border-crypto-yellow/30 bg-crypto-yellow/10 px-3 py-1 font-medium text-crypto-yellow">
                    {profile.xp.toLocaleString()} XP
                  </span>
                </div>
              </div>
            </div>

            <div className="mt-6 space-y-2">
              <div className="flex items-center justify-between text-sm">
                <span className="text-crypto-text-muted">XP Progress</span>
                <span className="font-medium">
                  {profile.xp.toLocaleString()} /{" "}
                  {profile.xpToNextLevel.toLocaleString()}
                </span>
              </div>
              <div className="h-3 w-full overflow-hidden rounded-full bg-crypto-surface2">
                <div
                  className="h-full rounded-full bg-gradient-to-r from-crypto-accent to-crypto-blue transition-all duration-500"
                  style={{ width: `${xpPercent}%` }}
                />
              </div>
              <p className="text-xs text-crypto-text-muted">
                {xpPercent < 100
                  ? `${Math.round(xpPercent)}% to next level`
                  : "Ready to level up!"}
              </p>
            </div>
          </div>

          <div className="card">
            <h3 className="mb-4 flex items-center gap-2 font-semibold">
              <Award className="h-5 w-5 text-crypto-yellow" />
              Badges
            </h3>
            <div className="grid grid-cols-4 gap-4 sm:grid-cols-6 md:grid-cols-8">
              {profile.badges.map((badge) => {
                const Icon = badgeIcons[badge.icon] || Shield;
                return (
                  <div
                    key={badge.id}
                    className={`flex flex-col items-center gap-1 rounded-lg p-3 text-center ${
                      badge.unlocked
                        ? "bg-crypto-accent/5"
                        : "opacity-30 grayscale"
                    }`}
                  >
                    <Icon className="h-6 w-6 text-crypto-accent" />
                    <span className="text-xs text-crypto-text-muted">
                      {badge.name}
                    </span>
                  </div>
                );
              })}
              {profile.badges.length === 0 && (
                <p className="col-span-full text-sm text-crypto-text-muted">
                  No badges yet. Start solving challenges to earn them!
                </p>
              )}
            </div>
          </div>
        </>
      )}
    </div>
  );
}
