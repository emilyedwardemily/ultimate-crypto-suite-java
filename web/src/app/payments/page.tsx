"use client";

import { useState } from "react";
import { CreditCard, Zap, Shield, Crown, Check } from "lucide-react";
import { stkPush } from "@/lib/api";

const plans = [
  {
    name: "Starter",
    price: 0,
    features: ["Basic encryption", "5 API calls/min", "Community support"],
    icon: Shield,
  },
  {
    name: "Pro",
    price: 9.99,
    features: [
      "Advanced encryption",
      "100 API calls/min",
      "CTF Academy access",
      "Priority support",
    ],
    icon: Zap,
    popular: true,
  },
  {
    name: "Enterprise",
    price: 29.99,
    features: [
      "All algorithms",
      "Unlimited API calls",
      "Shamir secrets",
      "Dedicated support",
    ],
    icon: Crown,
  },
];

export default function PaymentsPage() {
  const [phone, setPhone] = useState("");
  const [selectedPlan, setSelectedPlan] = useState("Pro");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);

  const handleSubscribe = async () => {
    if (!phone) return;
    setLoading(true);
    setError(null);
    setSuccess(false);
    try {
      const plan = plans.find((p) => p.name === selectedPlan);
      await stkPush(phone, String(plan?.price || 0));
      setSuccess(true);
    } catch (err: unknown) {
      const msg =
        err instanceof Error ? err.message : "STK push failed";
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Premium Subscriptions</h1>
        <p className="mt-1 text-sm text-crypto-text-muted">
          Unlock the full power of Ultimate Crypto Suite.
        </p>
      </div>

      {error && (
        <div className="rounded-lg border border-crypto-red/30 bg-crypto-red/10 p-4 text-sm text-crypto-red">
          {error}
        </div>
      )}

      {success && (
        <div className="rounded-lg border border-crypto-accent/30 bg-crypto-accent/10 p-4 text-sm text-crypto-accent">
          STK push sent to {phone}. Check your phone to complete the
          payment.
        </div>
      )}

      <div className="grid gap-4 md:grid-cols-3">
        {plans.map((plan) => {
          const Icon = plan.icon;
          const isSelected = selectedPlan === plan.name;
          return (
            <div
              key={plan.name}
              onClick={() => setSelectedPlan(plan.name)}
              className={`card relative cursor-pointer transition-all ${
                isSelected
                  ? "border-crypto-accent ring-1 ring-crypto-accent"
                  : ""
              } ${plan.popular ? "md:scale-105" : ""}`}
            >
              {plan.popular && (
                <span className="absolute -top-2.5 left-1/2 -translate-x-1/2 rounded-full bg-crypto-accent px-3 py-0.5 text-xs font-semibold text-white">
                  Popular
                </span>
              )}
              <div className="flex flex-col items-center text-center">
                <Icon className="h-8 w-8 text-crypto-accent" />
                <h3 className="mt-3 text-lg font-bold">{plan.name}</h3>
                <p className="mt-1 text-3xl font-bold">
                  ${plan.price}
                  <span className="text-sm font-normal text-crypto-text-muted">
                    {plan.price > 0 ? "/mo" : ""}
                  </span>
                </p>
                <ul className="mt-4 space-y-2 text-left">
                  {plan.features.map((f) => (
                    <li
                      key={f}
                      className="flex items-center gap-2 text-sm text-crypto-text-muted"
                    >
                      <Check className="h-4 w-4 shrink-0 text-crypto-accent" />
                      {f}
                    </li>
                  ))}
                </ul>
                {isSelected && (
                  <div className="mt-3 text-xs text-crypto-accent">
                    Selected
                  </div>
                )}
              </div>
            </div>
          );
        })}
      </div>

      {selectedPlan !== "Starter" && (
        <div className="card max-w-md space-y-4">
          <h3 className="flex items-center gap-2 font-semibold">
            <CreditCard className="h-5 w-5 text-crypto-accent" />
            Pay with M-Pesa (STK Push)
          </h3>
          <div>
            <label className="mb-1 block text-sm text-crypto-text-muted">
              Phone Number
            </label>
            <input
              type="tel"
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
              className="input-field"
              placeholder="254712345678"
            />
          </div>
          <button
            onClick={handleSubscribe}
            disabled={loading || !phone}
            className="btn-primary w-full"
          >
            {loading
              ? "Sending STK Push..."
              : `Pay $${plans.find((p) => p.name === selectedPlan)?.price} via M-Pesa`}
          </button>
        </div>
      )}
    </div>
  );
}
