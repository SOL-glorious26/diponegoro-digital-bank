import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import { api, clearTokens, hasRefreshToken, refreshSession, setSessionEndedHandler, setTokens } from "../api/client";
import { deviceId, deviceName } from "../lib/format";

const AuthCtx = createContext(null);

// Status sesi: "loading" saat memulihkan sesi dari refresh token, lalu "in" atau "out".
export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [state, setState] = useState("loading");
  const [notice, setNotice] = useState(null);

  const endSession = useCallback((message) => {
    clearTokens();
    setUser(null);
    setState("out");
    if (message) setNotice(message);
  }, []);

  useEffect(() => {
    setSessionEndedHandler(() => endSession("Sesi Anda telah berakhir. Silakan masuk kembali."));
    (async () => {
      if (!hasRefreshToken()) return setState("out");
      try {
        const t = await refreshSession();
        setUser(t.user ?? (await api.get("/me")).data);
        setState("in");
      } catch (ex) {
        // Refresh token ditolak (mis. logout dari semua perangkat atau sesi kedaluwarsa).
        endSession(ex?.response ? "Sesi Anda telah berakhir. Silakan masuk kembali." : null);
      }
    })();
  }, [endSession]);

  const acceptTokens = useCallback((tokens) => {
    setTokens(tokens);
    setUser(tokens.user);
    setNotice(null);
    setState("in");
  }, []);

  const login = useCallback(async (identifier, password) => {
    const { data } = await api.post("/auth/login", { identifier, password, deviceId: deviceId(), deviceName: deviceName() });
    acceptTokens(data);
    return data.user;
  }, [acceptTokens]);

  const reloadUser = useCallback(async () => {
    const { data } = await api.get("/me");
    setUser(data);
    return data;
  }, []);

  const logout = useCallback(async (all = false) => {
    try { await api.post(all ? "/auth/logout-all" : "/auth/logout"); } catch { /* sesi tetap diakhiri */ }
    endSession(all ? "Anda telah keluar dari semua perangkat." : null);
  }, [endSession]);

  const value = useMemo(() => ({ user, state, notice, setNotice, login, logout, acceptTokens, reloadUser, setUser }),
    [user, state, notice, login, logout, acceptTokens, reloadUser]);
  return <AuthCtx.Provider value={value}>{children}</AuthCtx.Provider>;
}

export const useAuth = () => useContext(AuthCtx);

// Tahap onboarding: e-KYC disetujui dan PIN sudah dibuat sebelum fitur transaksi dibuka (FR-02, FR-04).
export function onboardingStep(user) {
  if (!user) return "login";
  if (user.ekycStatus !== "APPROVED") return "ekyc";
  if (!user.hasPin) return "pin";
  return "done";
}
