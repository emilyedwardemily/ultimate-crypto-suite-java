"use client";

import { useState } from "react";
import {
  Beaker,
  ArrowRightLeft,
  Key,
  FileText,
  PenTool,
  Share2,
} from "lucide-react";
import {
  encrypt,
  decrypt,
  caesarCipher,
  legacyCipher,
  signData,
  verifySignature,
  splitSecret,
  reconstructSecret,
} from "@/lib/api";

interface Share {
  index: number;
  value: string;
}

const algorithms = [
  { id: "caesar", label: "Caesar", icon: Key },
  { id: "xor", label: "XOR", icon: ArrowRightLeft },
  { id: "atbash", label: "Atbash", icon: FileText },
  { id: "vigenere", label: "Vigenere", icon: Beaker },
  { id: "sign", label: "Sign", icon: PenTool },
  { id: "shamir", label: "Shamir", icon: Share2 },
];

export default function PlaygroundPage() {
  const [activeAlgo, setActiveAlgo] = useState("caesar");
  const [input, setInput] = useState("");
  const [key, setKey] = useState("");
  const [output, setOutput] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Sign tool state
  const [signature, setSignature] = useState("");
  const [verifyResult, setVerifyResult] = useState<boolean | null>(null);

  // Shamir tool state
  const [totalShares, setTotalShares] = useState(5);
  const [threshold, setThreshold] = useState(3);
  const [shares, setShares] = useState<Share[]>([]);
  const [sharesInput, setSharesInput] = useState("");

  const routeAlgorithm = async (mode: "encrypt" | "decrypt") => {
    setLoading(true);
    setError(null);
    try {
      let res;
      const shift = parseInt(key) || 3;
      if (activeAlgo === "caesar") {
        res = await caesarCipher(input, mode === "decrypt" ? -shift : shift);
      } else if (activeAlgo === "atbash" || activeAlgo === "vigenere") {
        res = await legacyCipher({
          text: input,
          shift: mode === "decrypt" ? -shift : shift,
          type: activeAlgo === "vigenere" ? "vigenere" : "atbash",
          key: key || undefined,
        });
      } else if (activeAlgo === "xor") {
        res = mode === "encrypt"
          ? await encrypt(input, key || "secret")
          : await decrypt(input, key || "secret");
      } else {
        res = await encrypt(input, key);
      }
      setOutput(res.data.result || res.data.encrypted || res.data.decrypted || JSON.stringify(res.data));
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "Operation failed");
    } finally {
      setLoading(false);
    }
  };

  const handleSign = async () => {
    setLoading(true);
    setError(null);
    setVerifyResult(null);
    try {
      const res = await signData(input);
      setSignature(res.data.signature || "");
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "Signing failed");
    } finally {
      setLoading(false);
    }
  };

  const handleVerify = async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await verifySignature(input, signature);
      setVerifyResult(res.data.valid === true);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "Verification failed");
    } finally {
      setLoading(false);
    }
  };

  const handleSplit = async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await splitSecret(input, totalShares, threshold);
      setShares(res.data.shares || []);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "Split failed");
    } finally {
      setLoading(false);
    }
  };

  const handleReconstruct = async () => {
    setLoading(true);
    setError(null);
    try {
      let parsedShares: Share[];
      if (sharesInput.trim()) {
        parsedShares = JSON.parse(sharesInput.trim());
      } else {
        parsedShares = shares;
      }
      const res = await reconstructSecret(parsedShares);
      setOutput(res.data.secret || "");
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "Reconstruction failed — check share JSON");
    } finally {
      setLoading(false);
    }
  };

  const currentAlgo = algorithms.find((a) => a.id === activeAlgo);
  const isCipher = ["caesar", "xor", "atbash", "vigenere"].includes(activeAlgo);

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Algorithm Playground</h1>
        <p className="mt-1 text-sm text-crypto-text-muted">
          Experiment with classic ciphers, digital signing and secret sharing in
          real time.
        </p>
      </div>

      <div className="flex flex-wrap gap-2">
        {algorithms.map((algo) => {
          const Icon = algo.icon;
          return (
            <button
              key={algo.id}
              onClick={() => setActiveAlgo(algo.id)}
              className={`flex items-center gap-2 rounded-lg border px-4 py-2 text-sm font-medium transition-colors ${
                activeAlgo === algo.id
                  ? "border-crypto-accent bg-crypto-accent/10 text-crypto-accent"
                  : "border-crypto-border text-crypto-text-muted hover:border-crypto-text-muted"
              }`}
            >
              <Icon className="h-4 w-4" />
              {algo.label}
            </button>
          );
        })}
      </div>

      {error && (
        <div className="rounded-lg border border-crypto-red/30 bg-crypto-red/10 p-3 text-sm text-crypto-red">
          {error}
        </div>
      )}

      {activeAlgo === "sign" && (
        <div className="grid gap-6 lg:grid-cols-2">
          <div className="card space-y-4">
            <h3 className="font-semibold">Digital Signing (RSA-2048 / SHA-256)</h3>
            <div>
              <label className="mb-1 block text-sm text-crypto-text-muted">
                Message to sign
              </label>
              <textarea
                value={input}
                onChange={(e) => setInput(e.target.value)}
                className="input-field min-h-[120px] resize-y font-mono"
                placeholder="Enter the message to sign..."
              />
            </div>
            <button
              onClick={handleSign}
              disabled={loading || !input}
              className="btn-primary w-full"
            >
              {loading ? "Signing..." : "Generate Signature"}
            </button>
          </div>

          <div className="card space-y-4">
            <h3 className="font-semibold">Signature</h3>
            <textarea
              value={signature}
              readOnly
              className="input-field min-h-[120px] resize-y font-mono text-crypto-accent break-all"
              placeholder="Signature will appear here..."
            />
            {signature && (
              <div className="space-y-3">
                <button
                  onClick={handleVerify}
                  disabled={loading}
                  className="w-full rounded-lg bg-crypto-blue px-4 py-2 text-sm font-semibold text-white transition-colors hover:bg-blue-600 disabled:opacity-50"
                >
                  {loading ? "Verifying..." : "Verify Signature"}
                </button>
                {verifyResult !== null && (
                  <div
                    className={`rounded-lg border p-3 text-sm ${
                      verifyResult
                        ? "border-crypto-accent/30 bg-crypto-accent/10 text-crypto-accent"
                        : "border-crypto-red/30 bg-crypto-red/10 text-crypto-red"
                    }`}
                  >
                    {verifyResult
                      ? "Valid — the message has not been tampered with."
                      : "Invalid — the signature does not match the message."}
                  </div>
                )}
                <button
                  onClick={() => navigator.clipboard.writeText(signature)}
                  className="text-sm text-crypto-text-muted hover:text-crypto-accent"
                >
                  Copy signature
                </button>
              </div>
            )}
          </div>
        </div>
      )}

      {activeAlgo === "shamir" && (
        <div className="grid gap-6 lg:grid-cols-2">
          <div className="card space-y-4">
            <h3 className="font-semibold">Shamir Secret Sharing</h3>
            <div>
              <label className="mb-1 block text-sm text-crypto-text-muted">
                Secret
              </label>
              <textarea
                value={input}
                onChange={(e) => setInput(e.target.value)}
                className="input-field min-h-[100px] resize-y font-mono"
                placeholder="Secret to split into shares..."
              />
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="mb-1 block text-sm text-crypto-text-muted">
                  Total shares (n)
                </label>
                <input
                  type="number"
                  value={totalShares}
                  onChange={(e) => setTotalShares(parseInt(e.target.value) || 2)}
                  min={2}
                  className="input-field"
                />
              </div>
              <div>
                <label className="mb-1 block text-sm text-crypto-text-muted">
                  Threshold (k)
                </label>
                <input
                  type="number"
                  value={threshold}
                  onChange={(e) => setThreshold(parseInt(e.target.value) || 2)}
                  min={2}
                  className="input-field"
                />
              </div>
            </div>
            <button
              onClick={handleSplit}
              disabled={loading || !input}
              className="btn-primary w-full"
            >
              {loading ? "Splitting..." : "Split Secret"}
            </button>
          </div>

          <div className="card space-y-4">
            <h3 className="font-semibold">Shares & Reconstruct</h3>
            {shares.length > 0 ? (
              <div className="space-y-2">
                {shares.map((s) => (
                  <div
                    key={s.index}
                    className="rounded-lg bg-crypto-surface2 p-2 font-mono text-xs text-crypto-accent break-all"
                  >
                    <span className="text-crypto-text-muted">Share {s.index}: </span>
                    {s.value}
                  </div>
                ))}
              </div>
            ) : (
              <p className="text-sm text-crypto-text-muted">
                Split a secret to generate shares. Reconstruct needs at least k
                shares.
              </p>
            )}
            <div>
              <label className="mb-1 block text-sm text-crypto-text-muted">
                Or paste shares JSON to reconstruct
              </label>
              <textarea
                value={sharesInput}
                onChange={(e) => setSharesInput(e.target.value)}
                className="input-field min-h-[80px] resize-y font-mono"
                placeholder='[{"index":1,"value":"1:abcd..."}]'
              />
            </div>
            <button
              onClick={handleReconstruct}
              disabled={loading || (shares.length === 0 && !sharesInput.trim())}
              className="w-full rounded-lg bg-crypto-blue px-4 py-2 text-sm font-semibold text-white transition-colors hover:bg-blue-600 disabled:opacity-50"
            >
              {loading ? "Reconstructing..." : "Reconstruct Secret"}
            </button>
            {output && (
              <div>
                <label className="mb-1 block text-sm text-crypto-text-muted">
                  Recovered secret
                </label>
                <textarea
                  value={output}
                  readOnly
                  className="input-field min-h-[60px] resize-y font-mono text-crypto-accent"
                />
              </div>
            )}
          </div>
        </div>
      )}

      {isCipher && (
        <div className="grid gap-6 lg:grid-cols-2">
          <div className="card space-y-4">
            <h3 className="font-semibold">Input</h3>
            <div className="space-y-3">
              <div>
                <label className="mb-1 block text-sm text-crypto-text-muted">
                  {activeAlgo !== "atbash" ? `${currentAlgo?.label} Key` : ""}
                </label>
                {activeAlgo !== "atbash" && (
                  <input
                    type="text"
                    value={key}
                    onChange={(e) => setKey(e.target.value)}
                    className="input-field font-mono"
                    placeholder={
                      activeAlgo === "caesar" ? "Shift value (e.g. 3)" : "Key"
                    }
                  />
                )}
              </div>
              <div>
                <label className="mb-1 block text-sm text-crypto-text-muted">Text</label>
                <textarea
                  value={input}
                  onChange={(e) => setInput(e.target.value)}
                  className="input-field min-h-[120px] resize-y font-mono"
                  placeholder="Enter text to encrypt/decrypt..."
                />
              </div>
              <div className="flex gap-3">
                <button
                  onClick={() => routeAlgorithm("encrypt")}
                  disabled={loading || !input}
                  className="btn-primary flex-1"
                >
                  {loading ? "Processing..." : "Encrypt"}
                </button>
                <button
                  onClick={() => routeAlgorithm("decrypt")}
                  disabled={loading || !input}
                  className="flex-1 rounded-lg bg-crypto-blue px-4 py-2 text-sm font-semibold text-white transition-colors hover:bg-blue-600 disabled:cursor-not-allowed disabled:opacity-50"
                >
                  Decrypt
                </button>
              </div>
            </div>
          </div>

          <div className="card space-y-4">
            <h3 className="font-semibold">Output</h3>
            <textarea
              value={output}
              readOnly
              className="input-field min-h-[200px] resize-y font-mono text-crypto-accent"
              placeholder="Result will appear here..."
            />
            {output && (
              <button
                onClick={() => navigator.clipboard.writeText(output)}
                className="text-sm text-crypto-text-muted hover:text-crypto-accent"
              >
                Copy to clipboard
              </button>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
