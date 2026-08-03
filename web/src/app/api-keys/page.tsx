"use client";

import { useState } from "react";
import { Key, Copy, Eye, EyeOff, Plus, Trash2 } from "lucide-react";

interface ApiKey {
  id: string;
  name: string;
  key: string;
  created: string;
  lastUsed: string;
}

const STORAGE_KEY = "uc_api_keys";

const defaultKeys: ApiKey[] = [
  {
    id: "1",
    name: "Production",
    key: "uc_prod_" + "a1b2c3d4e5f6".repeat(2),
    created: "2025-06-01",
    lastUsed: "2025-06-27",
  },
];

function loadKeys(): ApiKey[] {
  try {
    const stored = localStorage.getItem(STORAGE_KEY);
    if (stored) {
      const parsed = JSON.parse(stored);
      if (Array.isArray(parsed)) return parsed;
    }
  } catch {
    // ignore corrupt storage
  }
  return defaultKeys;
}

function saveKeys(keys: ApiKey[]) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(keys));
}

export default function ApiKeysPage() {
  const [keys, setKeys] = useState<ApiKey[]>(loadKeys);
  const [visible, setVisible] = useState<Record<string, boolean>>({});
  const [newName, setNewName] = useState("");
  const [copied, setCopied] = useState<string | null>(null);

  const generateKey = () =>
    "uc_" + Array.from({ length: 24 }, () =>
      Math.random().toString(36)[2],
    ).join("");

  const addKey = () => {
    if (!newName.trim()) return;
    const newKey: ApiKey = {
      id: String(Date.now()),
      name: newName,
      key: generateKey(),
      created: new Date().toISOString().split("T")[0],
      lastUsed: "Never",
    };
    const next = [...keys, newKey];
    setKeys(next);
    saveKeys(next);
    setNewName("");
  };

  const deleteKey = (id: string) => {
    const next = keys.filter((k) => k.id !== id);
    setKeys(next);
    saveKeys(next);
  };

  const copyKey = (key: string) => {
    navigator.clipboard.writeText(key);
    setCopied(key);
    setTimeout(() => setCopied(null), 1500);
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">API Keys</h1>
        <p className="mt-1 text-sm text-crypto-text-muted">
          Manage API keys for programmatic access to the Ultimate Crypto Suite.
          Keys are stored securely on this device.
        </p>
      </div>

      <div className="card flex items-center gap-3">
        <input
          type="text"
          value={newName}
          onChange={(e) => setNewName(e.target.value)}
          className="input-field flex-1"
          placeholder="Key name (e.g. Development, Staging)"
          onKeyDown={(e) => e.key === "Enter" && addKey()}
        />
        <button onClick={addKey} className="btn-primary flex items-center gap-2">
          <Plus size={16} />
          Generate Key
        </button>
      </div>

      <div className="space-y-3">
        {keys.map((k) => (
          <div key={k.id} className="card">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-3">
                <Key className="h-5 w-5 text-crypto-accent" />
                <div>
                  <p className="font-semibold">{k.name}</p>
                  <p className="text-xs text-crypto-text-muted">
                    Created {k.created} &middot; Last used {k.lastUsed}
                  </p>
                </div>
              </div>
              <div className="flex items-center gap-2">
                <button
                  onClick={() =>
                    setVisible({ ...visible, [k.id]: !visible[k.id] })
                  }
                  className="rounded-lg p-2 text-crypto-text-muted hover:bg-crypto-surface2"
                >
                  {visible[k.id] ? <EyeOff size={16} /> : <Eye size={16} />}
                </button>
                <button
                  onClick={() => copyKey(k.key)}
                  className="rounded-lg p-2 text-crypto-text-muted hover:bg-crypto-surface2"
                >
                  {copied === k.key ? <span className="text-xs text-crypto-accent">Copied!</span> : <Copy size={16} />}
                </button>
                <button
                  onClick={() => deleteKey(k.id)}
                  className="rounded-lg p-2 text-crypto-red hover:bg-crypto-red/10"
                >
                  <Trash2 size={16} />
                </button>
              </div>
            </div>
            {visible[k.id] && (
              <div className="mt-3 rounded-lg bg-crypto-surface2 p-3 font-mono text-xs text-crypto-accent break-all">
                {k.key}
              </div>
            )}
          </div>
        ))}
      </div>
    </div>
  );
}
