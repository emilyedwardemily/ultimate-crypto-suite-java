"use client";

import { useState, useEffect, useCallback } from "react";
import { RefreshCw, ScrollText } from "lucide-react";
import { getAuditLogs } from "@/lib/api";

interface AuditLog {
  operator_id?: string;
  action?: string;
  module?: string;
  timestamp?: string;
  status?: string;
}

export default function AuditLogsPage() {
  const [logs, setLogs] = useState<AuditLog[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchLogs = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await getAuditLogs();
      setLogs(res.data.logs || []);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "Failed to load audit logs");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchLogs();
  }, [fetchLogs]);

  const statusColor = (status?: string) => {
    const s = (status || "").toUpperCase();
    if (s.includes("SUCCESS") || s.includes("PREMIUM")) return "text-crypto-accent";
    if (s.includes("FAILED") || s.includes("ERROR")) return "text-crypto-red";
    return "text-crypto-text-muted";
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">Audit Logs</h1>
          <p className="mt-1 text-sm text-crypto-text-muted">
            Forensic trace of all operator actions recorded by the security
            backend.
          </p>
        </div>
        <button
          onClick={fetchLogs}
          disabled={loading}
          className="btn-primary flex items-center gap-2"
        >
          <RefreshCw size={16} className={loading ? "animate-spin" : ""} />
          Refresh
        </button>
      </div>

      {error && (
        <div className="rounded-lg border border-crypto-red/30 bg-crypto-red/10 p-3 text-sm text-crypto-red">
          {error}
        </div>
      )}

      {loading && logs.length === 0 ? (
        <div className="flex flex-col items-center justify-center gap-3 rounded-lg border border-crypto-border bg-crypto-surface py-16">
          <RefreshCw className="h-8 w-8 animate-spin text-crypto-text-muted" />
          <p className="text-sm text-crypto-text-muted">Loading forensic logs...</p>
        </div>
      ) : logs.length === 0 ? (
        <div className="flex flex-col items-center justify-center gap-3 rounded-lg border border-crypto-border bg-crypto-surface py-16">
          <ScrollText className="h-8 w-8 text-crypto-text-muted" />
          <p className="text-sm text-crypto-text-muted">
            No audit logs recorded yet.
          </p>
        </div>
      ) : (
        <div className="card overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead>
                <tr className="border-b border-crypto-border text-xs uppercase tracking-wider text-crypto-text-muted">
                  <th className="px-4 py-3">Operator</th>
                  <th className="px-4 py-3">Action</th>
                  <th className="px-4 py-3">Module</th>
                  <th className="px-4 py-3">Timestamp</th>
                  <th className="px-4 py-3">Status</th>
                </tr>
              </thead>
              <tbody>
                {logs.map((log, i) => (
                  <tr
                    key={i}
                    className="border-b border-crypto-border/50 last:border-0 hover:bg-crypto-surface2"
                  >
                    <td className="px-4 py-3 font-mono text-xs">
                      {log.operator_id || "—"}
                    </td>
                    <td className="px-4 py-3">{log.action || "—"}</td>
                    <td className="px-4 py-3 font-mono text-xs">
                      {log.module || "—"}
                    </td>
                    <td className="px-4 py-3 text-xs text-crypto-text-muted">
                      {log.timestamp ? log.timestamp.replace("T", " ").slice(0, 19) : "—"}
                    </td>
                    <td className={`px-4 py-3 ${statusColor(log.status)}`}>
                      {log.status || "AUDITED"}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
}
