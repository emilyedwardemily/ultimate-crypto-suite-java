"use client";

import { useState } from "react";
import { Beaker, ArrowRightLeft, Key, FileText } from "lucide-react";
import { encrypt, decrypt, caesarCipher, legacyCipher } from "@/lib/api";

const algorithms = [
  { id: "caesar", label: "Caesar", icon: Key },
  { id: "xor", label: "XOR", icon: ArrowRightLeft },
  { id: "atbash", label: "Atbash", icon: FileText },
  { id: "vigenere", label: "Vigenere", icon: Beaker },
];

export default function PlaygroundPage() {
  const [activeAlgo, setActiveAlgo] = useState("caesar");
  const [input, setInput] = useState("");
  const [key, setKey] = useState("");
  const [output, setOutput] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

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

  const currentAlgo = algorithms.find((a) => a.id === activeAlgo);

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Algorithm Playground</h1>
        <p className="mt-1 text-sm text-crypto-text-muted">
          Experiment with classic ciphers in real time.
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
    </div>
  );
}
