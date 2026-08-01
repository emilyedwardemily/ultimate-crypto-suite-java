import axios from "axios";

const PYTHON_URL = process.env.NEXT_PUBLIC_PYTHON_URL || "http://localhost:7900";
const NODE_URL = process.env.NEXT_PUBLIC_NODE_URL || "http://localhost:10000";
const API_KEY = process.env.NEXT_PUBLIC_API_KEY || "Emily_Crypto_Secure_2026_KIU";

const pythonApi = axios.create({
  baseURL: PYTHON_URL,
  headers: { "Content-Type": "application/json" },
});

const nodeApi = axios.create({
  baseURL: NODE_URL,
  headers: { "Content-Type": "application/json" },
});

pythonApi.interceptors.request.use((config) => {
  config.headers["X-API-Key"] = API_KEY;
  return config;
});

nodeApi.interceptors.request.use((config) => {
  config.headers["X-API-Key"] = API_KEY;
  return config;
});

export default pythonApi;

// ── AUTH (Node.js Gateway) ──────────────────────────
export const sendOtp = (email: string) =>
  nodeApi.post("/api/auth/send-otp", { email });

export const verifyOtp = (email: string, otp: string) =>
  nodeApi.post("/api/auth/verify-otp", { email, otp });

export const login = (email: string, password: string) =>
  nodeApi.post("/api/auth/login", { email, password });

export const register = (data: { email: string; password: string; name: string }) =>
  nodeApi.post("/api/auth/register", data);

// ── CRYPTOGRAPHY (Python Backend) ───────────────────
export const encrypt = (data: string, key?: string) =>
  pythonApi.post("/encrypt", { data, key });

export const decrypt = (data: string, key?: string) =>
  pythonApi.post("/decrypt", { data, key });

export const signData = (data: string) =>
  pythonApi.post("/sign", { data });

export const caesarCipher = (data: string, shift: number) =>
  pythonApi.post("/caesar", { data, shift });

export const legacyCipher = (data: { text: string; shift: number; type: string; key?: string }) =>
  pythonApi.post("/legacy-cipher", data);

// ── SHAMIR SECRET SHARING ───────────────────────────
export const splitSecret = (secret: string, n: number, k: number) =>
  pythonApi.post("/split", { secret, n, k });

export const reconstructSecret = (shares: { x: number; y: number }[]) =>
  pythonApi.post("/reconstruct", { shares });

// ── PAYMENTS ────────────────────────────────────────
export const stkPush = (phoneNumber: string, amount: string, email?: string) =>
  pythonApi.post("/api/v1/payments/stk-push", { phoneNumber, amount, email });

// ── AUDIT ───────────────────────────────────────────
export const auditLog = (data: { operator_id: string; action: string; module: string }) =>
  pythonApi.post("/audit-log", data);

export const getAuditLogs = () => pythonApi.get("/get-audit-logs");

// ── CTF / GAMIFICATION ─────────────────────────────
export const getCtfChallenges = (operatorId?: string) =>
  pythonApi.get("/ctf/challenges", { params: { operator_id: operatorId } });

export const submitCtfFlag = (operatorId: string, challengeId: string, flag: string) =>
  pythonApi.post("/ctf/submit", {
    operator_id: operatorId,
    challenge_id: challengeId,
    flag,
  });

export const getLeaderboard = () => pythonApi.get("/leaderboard");

export const getProfile = (operatorId?: string) =>
  pythonApi.get("/profile", { params: { operator_id: operatorId } });

export const getDashboardStats = () => pythonApi.get("/dashboard/stats");
