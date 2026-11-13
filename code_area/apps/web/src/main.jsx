import React from "react";
import ReactDOM from "react-dom/client";
import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import "./styles.css";
import { AuthProvider } from "./auth/AuthContext";
import { GuestOnly, RequireAuth } from "./auth/ProtectedRoute";
import Layout from "./components/Layout";
import { CreatePinPage, EkycPage, LoginPage, RecoveryPage, RegisterPage } from "./pages/AuthPages";
import Dashboard from "./pages/Dashboard";
import Transfer from "./pages/Transfer";
import History from "./pages/History";
import TopUp from "./pages/TopUp";
import Qris from "./pages/Qris";
import Pockets from "./pages/Pockets";
import Cards from "./pages/Cards";
import Profile from "./pages/Profile";
import Notifications from "./pages/Notifications";

function App() {
  return (
    <Routes>
      <Route element={<GuestOnly />}>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/daftar" element={<RegisterPage />} />
        <Route path="/lupa" element={<RecoveryPage />} />
      </Route>
      <Route element={<RequireAuth onboarding />}>
        <Route path="/ekyc" element={<EkycPage />} />
        <Route path="/pin/buat" element={<CreatePinPage />} />
      </Route>
      <Route element={<RequireAuth />}>
        <Route element={<Layout />}>
          <Route index element={<Dashboard />} />
          <Route path="/transfer" element={<Transfer />} />
          <Route path="/topup" element={<TopUp />} />
          <Route path="/qris" element={<Qris />} />
          <Route path="/riwayat" element={<History />} />
          <Route path="/kantong" element={<Pockets />} />
          <Route path="/kartu" element={<Cards />} />
          <Route path="/profil" element={<Profile />} />
          <Route path="/notifikasi" element={<Notifications />} />
        </Route>
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

ReactDOM.createRoot(document.getElementById("root")).render(
  <React.StrictMode>
    <BrowserRouter>
      <AuthProvider>
        <App />
      </AuthProvider>
    </BrowserRouter>
  </React.StrictMode>,
);
