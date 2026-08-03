"use client";

import {
  createContext,
  useContext,
  useState,
  useEffect,
  useCallback,
  type ReactNode,
} from "react";
import { login as apiLogin, register as apiRegister, sendOtp, verifyOtp } from "./api";

interface User {
  email: string;
  name: string;
  token: string;
  operatorId: string;
}

interface AuthContextType {
  user: User | null;
  loading: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (email: string, password: string, name: string) => Promise<void>;
  sendOtp: (email: string) => Promise<void>;
  verifyOtp: (email: string, otp: string) => Promise<void>;
  logout: () => void;
  otpStep: boolean;
  setOtpStep: (v: boolean) => void;
}

const AuthContext = createContext<AuthContextType | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  const [otpStep, setOtpStep] = useState(false);

  useEffect(() => {
    try {
      const stored = localStorage.getItem("uc_user");
      if (stored) setUser(JSON.parse(stored));
    } catch {
      localStorage.removeItem("uc_user");
    }
    setLoading(false);
  }, []);

  const login = useCallback(async (email: string, password: string) => {
    const res = await apiLogin(email, password);
    const u: User = {
      email,
      name: res.data.name || email.split("@")[0],
      token: res.data.token,
      operatorId: res.data.operatorId || email,
    };
    localStorage.setItem("uc_user", JSON.stringify(u));
    setUser(u);
  }, []);

  const register = useCallback(
    async (email: string, password: string, name: string) => {
      const res = await apiRegister({ email, password, username: name });
      const username = res.data.user?.username || name;
      const u: User = {
        email,
        name: username,
        token: res.data.token,
        operatorId: res.data.user?.username || email,
      };
      localStorage.setItem("uc_user", JSON.stringify(u));
      setUser(u);
    },
    [],
  );

  const handleSendOtp = useCallback(async (email: string) => {
    await sendOtp(email);
    setOtpStep(true);
  }, []);

  const handleVerifyOtp = useCallback(
    async (email: string, otp: string) => {
      await verifyOtp(email, otp);
    },
    [],
  );

  const logout = useCallback(() => {
    localStorage.removeItem("uc_user");
    setUser(null);
    setOtpStep(false);
  }, []);

  return (
    <AuthContext.Provider
      value={{
        user,
        loading,
        login,
        register,
        sendOtp: handleSendOtp,
        verifyOtp: handleVerifyOtp,
        logout,
        otpStep,
        setOtpStep,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}
