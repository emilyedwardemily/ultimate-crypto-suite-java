import axios from "axios";

const PYTHON_URL =
  process.env.NEXT_PUBLIC_PYTHON_URL || "https://ultimate-crypto-python.onrender.com";
const NODE_URL =
  process.env.NEXT_PUBLIC_NODE_URL || "https://ultimate-crypto-node-gateway.onrender.com";
const API_KEY =
  process.env.NEXT_PUBLIC_API_KEY || "Emily_Crypto_Secure_2026_KIU";

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

export const register = (data: { email: string; password: string; username: string }) =>
  nodeApi.post("/api/auth/register", data);

// ── CRYPTOGRAPHY (Python Backend) ───────────────────
export const encrypt = (data: string, key?: string) =>
  pythonApi.post("/encrypt", { data, key });

export const decrypt = (data: string, key?: string) =>
  pythonApi.post("/decrypt", { data, key });

export const signData = (data: string) =>
  pythonApi.post("/sign", { data });

export const verifySignature = (data: string, signature: string) =>
  pythonApi.post("/verify-signature", { data, signature });

export const caesarCipher = (data: string, shift: number) =>
  pythonApi.post("/caesar", { data, shift });

export const legacyCipher = (data: { text: string; shift: number; type: string; key?: string }) =>
  pythonApi.post("/legacy-cipher", data);

// ── SHAMIR SECRET SHARING ───────────────────────────
export const splitSecret = (secret: string, n: number, k: number) =>
  pythonApi.post("/split", { secret, n, k });

export const reconstructSecret = (shares: { index: number; value: string }[]) =>
  pythonApi.post("/reconstruct", { shares });

// ── AUDIT ───────────────────────────────────────────
export const auditLog = (data: { operator_id: string; action: string; module: string }) =>
  pythonApi.post("/audit-log", data);

export const getAuditLogs = () => pythonApi.get("/get-audit-logs");

// ── ANTI-FORENSICS / UTILITIES (Python Backend) ─────
export const secureWipe = (filePath: string) =>
  pythonApi.post("/secure-wipe", { file_path: filePath });

export const saveImage = (imageData: string) =>
  pythonApi.post("/save-image", { image_data: imageData });

export const sendSecureEmail = (to: string, content: string) =>
  pythonApi.post("/send-secure-email", { to, content });

export const verifyLicense = (licenseKey: string) =>
  pythonApi.post("/verify-license", { license_key: licenseKey });

// ── VAULT (Node.js Gateway) ─────────────────────────
export const vaultSync = (data: {
  userId: string;
  service: string;
  encryptedData: string;
  type?: string;
}) => nodeApi.post("/api/vault/sync", data);

export const vaultFetch = (userId: string) =>
  nodeApi.get(`/api/vault/fetch/${userId}`);

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
