"use client";

import { useEffect, useState } from "react";
import { Swords, Flag, CheckCircle, Clock } from "lucide-react";
import { getCtfChallenges, submitCtfFlag } from "@/lib/api";

const OPERATOR_ID = "UC-PRO-71468B1B";

interface Challenge {
  id: string;
  title: string;
  category: string;
  difficulty: "easy" | "medium" | "hard" | "insane";
  points: number;
  solved: boolean;
  description: string;
}

const difficultyStyles: Record<string, string> = {
  easy: "bg-crypto-accent/10 text-crypto-accent border-crypto-accent/30",
  medium: "bg-crypto-yellow/10 text-crypto-yellow border-crypto-yellow/30",
  hard: "bg-crypto-red/10 text-crypto-red border-crypto-red/30",
  insane: "bg-crypto-purple/10 text-crypto-purple border-crypto-purple/30",
};

const categories = [
  "All",
  "Cryptography",
  "Reverse Engineering",
  "Web",
  "Forensics",
  "OSINT",
];

export default function CtfPage() {
  const [challenges, setChallenges] = useState<Challenge[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [activeCategory, setActiveCategory] = useState("All");
  const [flagInputs, setFlagInputs] = useState<Record<string, string>>({});
  const [submitting, setSubmitting] = useState<string | null>(null);

  useEffect(() => {
    getCtfChallenges(OPERATOR_ID)
      .then((res) => setChallenges(res.data.challenges || res.data))
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, []);

  const handleSubmit = async (challengeId: string) => {
    const flag = flagInputs[challengeId];
    if (!flag) return;
    setSubmitting(challengeId);
    setError(null);
    try {
      const res = await submitCtfFlag(OPERATOR_ID, challengeId, flag);
      if (res.data.status === "success") {
        setChallenges((prev) =>
          prev.map((c) => (c.id === challengeId ? { ...c, solved: true } : c))
        );
      } else {
        setError(res.data.message || "Wrong flag");
      }
    } catch {
      setError("Failed to submit flag");
    } finally {
      setSubmitting(null);
    }
  };

  const filtered =
    activeCategory === "All"
      ? challenges
      : challenges.filter((c) => c.category === activeCategory);

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">CTF Academy</h1>
        <p className="mt-1 text-sm text-crypto-text-muted">
          Sharpen your skills with hands-on cryptography challenges.
        </p>
      </div>

      {error && (
        <div className="rounded-lg border border-crypto-red/30 bg-crypto-red/10 p-4 text-sm text-crypto-red">
          {error}
        </div>
      )}

      <div className="flex flex-wrap gap-2">
        {categories.map((cat) => (
          <button
            key={cat}
            onClick={() => setActiveCategory(cat)}
            className={`rounded-lg border px-3 py-1.5 text-sm font-medium transition-colors ${
              activeCategory === cat
                ? "border-crypto-accent bg-crypto-accent/10 text-crypto-accent"
                : "border-crypto-border text-crypto-text-muted hover:border-crypto-text-muted"
            }`}
          >
            {cat}
          </button>
        ))}
      </div>

      {loading ? (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {Array.from({ length: 6 }).map((_, i) => (
            <div key={i} className="card animate-pulse space-y-3">
              <div className="h-4 w-3/4 rounded bg-crypto-surface2" />
              <div className="h-3 w-1/2 rounded bg-crypto-surface2" />
              <div className="h-3 w-full rounded bg-crypto-surface2" />
            </div>
          ))}
        </div>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {filtered.map((challenge) => (
            <div key={challenge.id} className="card space-y-3">
              <div className="flex items-start justify-between">
                <div className="flex items-center gap-2">
                  <Swords className="h-5 w-5 text-crypto-accent" />
                  <h3 className="font-semibold">{challenge.title}</h3>
                </div>
                {challenge.solved && (
                  <CheckCircle className="h-5 w-5 text-crypto-accent" />
                )}
              </div>
              <span
                className={`inline-block rounded border px-2 py-0.5 text-xs font-medium capitalize ${
                  difficultyStyles[challenge.difficulty]
                }`}
              >
                {challenge.difficulty}
              </span>
              <p className="line-clamp-2 text-sm text-crypto-text-muted">
                {challenge.description}
              </p>
              <div className="flex items-center justify-between text-sm">
                <span className="flex items-center gap-1 text-crypto-yellow">
                  <Flag className="h-4 w-4" />
                  {challenge.points} pts
                </span>
                <span className="flex items-center gap-1 text-crypto-text-muted">
                  <Clock className="h-4 w-4" />
                  {challenge.category}
                </span>
              </div>
              <div className="flex gap-2">
                <input
                  type="text"
                  placeholder="flag{...}"
                  value={flagInputs[challenge.id] || ""}
                  onChange={(e) =>
                    setFlagInputs((prev) => ({
                      ...prev,
                      [challenge.id]: e.target.value,
                    }))
                  }
                  className="input-field flex-1 font-mono text-xs"
                />
                <button
                  onClick={() => handleSubmit(challenge.id)}
                  disabled={submitting === challenge.id || challenge.solved}
                  className="btn-primary px-3 text-xs"
                >
                  {submitting === challenge.id
                    ? "Checking..."
                    : challenge.solved
                      ? "Solved"
                      : "Submit"}
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
