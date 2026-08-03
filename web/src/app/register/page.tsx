"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { Shield, Mail, Lock, User, KeyRound } from "lucide-react";
import { useAuth } from "@/lib/auth";

export default function RegisterPage() {
  const router = useRouter();
  const { register, sendOtp, verifyOtp, otpStep, setOtpStep } = useAuth();
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [otp, setOtp] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  const handleRegister = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    try {
      await register(email, password, name);
      setMessage("Account created. Sending verification OTP...");
      await sendOtp(email);
      setMessage("OTP sent to your registered email.");
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "Registration failed";
      setError(msg);
      setLoading(false);
      return;
    }
    setLoading(false);
  };

  const handleVerifyOtp = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    try {
      await verifyOtp(email, otp);
      setMessage("Verified! Redirecting to dashboard...");
      setTimeout(() => router.push("/"), 1000);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "Invalid OTP";
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex min-h-[80vh] items-center justify-center">
      <div className="w-full max-w-md space-y-6">
        <div className="text-center">
          <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-crypto-accent/10">
            <Shield className="h-6 w-6 text-crypto-accent" />
          </div>
          <h1 className="mt-4 text-2xl font-bold">Create Account</h1>
          <p className="text-sm text-crypto-text-muted">
            Join the Ultimate Crypto Suite security platform
          </p>
        </div>

        {error && (
          <div className="rounded-lg border border-crypto-red/30 bg-crypto-red/10 p-3 text-sm text-crypto-red">
            {error}
          </div>
        )}

        {message && otpStep && (
          <div className="rounded-lg border border-crypto-accent/30 bg-crypto-accent/10 p-3 text-sm text-crypto-accent">
            {message}
          </div>
        )}

        {!otpStep ? (
          <form onSubmit={handleRegister} className="space-y-4">
            <div className="space-y-2">
              <label className="text-sm font-medium text-crypto-text-muted">
                Username
              </label>
              <div className="relative">
                <User className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-crypto-text-muted" />
                <input
                  type="text"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  className="input-field pl-10"
                  placeholder="e.g. CryptoNinja"
                  required
                />
              </div>
            </div>
            <div className="space-y-2">
              <label className="text-sm font-medium text-crypto-text-muted">
                Email
              </label>
              <div className="relative">
                <Mail className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-crypto-text-muted" />
                <input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className="input-field pl-10"
                  placeholder="you@example.com"
                  required
                />
              </div>
            </div>
            <div className="space-y-2">
              <label className="text-sm font-medium text-crypto-text-muted">
                Password
              </label>
              <div className="relative">
                <Lock className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-crypto-text-muted" />
                <input
                  type="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  className="input-field pl-10"
                  placeholder="••••••••"
                  minLength={6}
                  required
                />
              </div>
            </div>
            <button
              type="submit"
              disabled={loading}
              className="btn-primary w-full"
            >
              {loading ? "Creating account..." : "Create Account"}
            </button>
            <p className="text-center text-sm text-crypto-text-muted">
              Already have an account?{" "}
              <Link href="/login" className="text-crypto-accent hover:underline">
                Sign in
              </Link>
            </p>
          </form>
        ) : (
          <form onSubmit={handleVerifyOtp} className="space-y-4">
            <div className="space-y-2">
              <label className="text-sm font-medium text-crypto-text-muted">
                One-Time Password
              </label>
              <div className="relative">
                <KeyRound className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-crypto-text-muted" />
                <input
                  type="text"
                  value={otp}
                  onChange={(e) => setOtp(e.target.value)}
                  className="input-field pl-10 text-center font-mono text-lg tracking-widest"
                  placeholder="000000"
                  maxLength={6}
                  required
                />
              </div>
            </div>
            <button
              type="submit"
              disabled={loading}
              className="btn-primary w-full"
            >
              {loading ? "Verifying..." : "Verify & Activate"}
            </button>
            <button
              type="button"
              onClick={() => {
                setOtpStep(false);
                setOtp("");
                setMessage(null);
              }}
              className="w-full text-center text-sm text-crypto-text-muted hover:text-crypto-accent"
            >
              Back to registration
            </button>
          </form>
        )}
      </div>
    </div>
  );
}
