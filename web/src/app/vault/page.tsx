"use client";

import { useState, useEffect, useCallback } from "react";
import { Save, RefreshCw, Lock, Vault as VaultIcon } from "lucide-react";
import { useAuth } from "@/lib/auth";
import { encrypt, vaultSync, vaultFetch } from "@/lib/api";

interface VaultEntry {
  _id: string;
  service?: string;
  encryptedData?: string;
  type?: string;
  date?: string;
}

export default function VaultPage() {
  const { user } = useAuth();
  const userId = user?.email || user?.operatorId || "UC-PRO-71468B1B";

  const [service, setService] = useState("");
  const [secret, setSecret] = useState("");
  const [vaultKey, setVaultKey] = useState("");
  const [entries, setEntries] = useState<VaultEntry[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  const fetchVault = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await vaultFetch(userId);
      setEntries(Array.isArray(res.data) ? res.data : []);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "Failed to load vault");
      setEntries([]);
    } finally {
      setLoading(false);
    }
  }, [userId]);

  useEffect(() => {
    fetchVault();
  }, [fetchVault]);

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!service.trim() || !secret.trim()) return;
    setLoading(true);
    setError(null);
    setMessage(null);
    try {
      // Encrypt locally through the backend first, then sync to the vault
      const enc = await encrypt(secret, vaultKey || "vault-secret");
      const encryptedData = enc.data.result || enc.data.encrypted || String(enc.data);
      await vaultSync({
        userId,
        service: service.trim(),
        encryptedData: String(encryptedData),
        type: "Vault",
      });
      setMessage(`"${service}" saved to vault (encrypted).`);
      setService("");
      setSecret("");
      await fetchVault();
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "Failed to save to vault");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Secure Vault</h1>
        <p className="mt-1 text-sm text-crypto-text-muted">
          Encrypt secrets locally and sync them to your cloud vault.
        </p>
      </div>

      {message && (
        <div className="rounded-lg border border-crypto-accent/30 bg-crypto-accent/10 p-3 text-sm text-crypto-accent">
          {message}
        </div>
      )}
      {error && (
        <div className="rounded-lg border border-crypto-red/30 bg-crypto-red/10 p-3 text-sm text-crypto-red">
          {error}
        </div>
      )}

      <form onSubmit={handleSave} className="card space-y-4">
        <h3 className="flex items-center gap-2 font-semibold">
          <Lock size={16} className="text-crypto-accent" />
          Add New Entry
        </h3>
        <div className="grid gap-3 md:grid-cols-2">
          <div>
            <label className="mb-1 block text-sm text-crypto-text-muted">
              Service / Label
            </label>
            <input
              type="text"
              value={service}
              onChange={(e) => setService(e.target.value)}
              className="input-field"
              placeholder="e.g. GitHub, Bank, Wi-Fi"
              required
            />
          </div>
          <div>
            <label className="mb-1 block text-sm text-crypto-text-muted">
              Encryption Key (optional)
            </label>
            <input
              type="password"
              value={vaultKey}
              onChange={(e) => setVaultKey(e.target.value)}
              className="input-field"
              placeholder="Passphrase for AES-256"
            />
          </div>
        </div>
        <div>
          <label className="mb-1 block text-sm text-crypto-text-muted">
            Secret / Credential
          </label>
          <textarea
            value={secret}
            onChange={(e) => setSecret(e.target.value)}
            className="input-field min-h-[80px] resize-y font-mono"
            placeholder="The secret will be AES-256 encrypted before sync..."
            required
          />
        </div>
        <button
          type="submit"
          disabled={loading}
          className="btn-primary flex w-fit items-center gap-2"
        >
          <Save size={16} />
          {loading ? "Encrypting & Syncing..." : "Encrypt & Save to Vault"}
        </button>
      </form>

      <div className="flex items-center justify-between">
        <h3 className="font-semibold">Vault Entries</h3>
        <button
          onClick={fetchVault}
          disabled={loading}
          className="flex items-center gap-2 text-sm text-crypto-text-muted hover:text-crypto-accent"
        >
          <RefreshCw size={14} className={loading ? "animate-spin" : ""} />
          Refresh
        </button>
      </div>

      {entries.length === 0 && !loading ? (
        <div className="flex flex-col items-center justify-center gap-3 rounded-lg border border-crypto-border bg-crypto-surface py-16">
          <VaultIcon className="h-8 w-8 text-crypto-text-muted" />
          <p className="text-sm text-crypto-text-muted">
            No vault entries yet. Save your first encrypted secret above.
          </p>
        </div>
      ) : (
        <div className="space-y-3">
          {entries.map((entry) => (
            <div key={entry._id} className="card">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <Lock className="h-5 w-5 text-crypto-accent" />
                  <div>
                    <p className="font-semibold">{entry.service || "General"}</p>
                    <p className="text-xs text-crypto-text-muted">
                      {entry.type || "Vault"} &middot;{" "}
                      {entry.date
                        ? new Date(entry.date).toLocaleString()
                        : "Unknown date"}
                    </p>
                  </div>
                </div>
              </div>
              {entry.encryptedData && (
                <div className="mt-3 rounded-lg bg-crypto-surface2 p-3 font-mono text-xs text-crypto-accent break-all">
                  {entry.encryptedData}
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
