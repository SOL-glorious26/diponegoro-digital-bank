import { Navigate, Outlet, useLocation } from "react-router-dom";
import { onboardingStep, useAuth } from "./AuthContext";
import { Spinner } from "../components/ui";

// Route guard (SAD-DDB-001 8.3): harus login, lalu e-KYC disetujui dan PIN sudah dibuat.
export function RequireAuth({ onboarding = false }) {
  const { user, state } = useAuth();
  const loc = useLocation();
  if (state === "loading") return <div className="center-page"><Spinner label="Memulihkan sesi…" /></div>;
  if (state !== "in") return <Navigate to="/login" replace state={{ from: loc.pathname }} />;
  const step = onboardingStep(user);
  if (!onboarding && step !== "done") return <Navigate to={step === "ekyc" ? "/ekyc" : "/pin/buat"} replace />;
  if (onboarding && step === "done") return <Navigate to="/" replace />;
  return <Outlet />;
}

export function GuestOnly() {
  const { state } = useAuth();
  if (state === "loading") return <div className="center-page"><Spinner label="Memuat…" /></div>;
  if (state === "in") return <Navigate to="/" replace />;
  return <Outlet />;
}
