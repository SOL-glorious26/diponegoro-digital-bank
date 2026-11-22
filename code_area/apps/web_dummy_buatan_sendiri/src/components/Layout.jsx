import { useEffect, useState } from "react";
import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { Bell, CreditCard, History, Home, LogOut, Menu, PiggyBank, QrCode, Send, Smartphone, User, X } from "lucide-react";
import { useAuth } from "../auth/AuthContext";
import { api } from "../api/client";
import Avatar from "./Avatar";

const NAV = [
  { to: "/", label: "Beranda", icon: Home, end: true },
  { to: "/transfer", label: "Transfer", icon: Send },
  { to: "/topup", label: "Top-up & Tagihan", icon: Smartphone },
  { to: "/qris", label: "QRIS Saya", icon: QrCode },
  { to: "/riwayat", label: "Riwayat", icon: History },
  { to: "/kantong", label: "Kantong", icon: PiggyBank },
  { to: "/kartu", label: "Virtual Card", icon: CreditCard },
  { to: "/profil", label: "Profil & Keamanan", icon: User },
];

export function Brand({ light }) {
  return (
    <div className={`brand ${light ? "brand-light" : ""}`}>
      <img className="brand-mark" src="/logo-mark.png" alt="Logo Diponegoro" width={40} height={40} />
      <div>
        <strong>Diponegoro</strong>
        <span>Digital Bank</span>
      </div>
    </div>
  );
}

export default function Layout() {
  const { user, logout } = useAuth();
  const [open, setOpen] = useState(false);
  const [unread, setUnread] = useState(0);
  const nav = useNavigate();

  // Jumlah notifikasi belum dibaca (FR-29), diperbarui berkala.
  useEffect(() => {
    let alive = true;
    const load = () => api.get("/me/notifications/unread-count")
      .then((r) => alive && setUnread(r.data?.unread ?? r.data?.count ?? 0)).catch(() => {});
    load();
    const t = setInterval(load, 30000);
    return () => { alive = false; clearInterval(t); };
  }, []);

  return (
    <div className={`shell ${open ? "nav-open" : ""}`}>
      <aside className="sidebar">
        <div className="sidebar-top">
          <Brand light />
          <button className="icon-btn only-mobile light" onClick={() => setOpen(false)} aria-label="Tutup menu"><X /></button>
        </div>
        <nav>
          {NAV.map(({ to, label, icon: Icon, end }) => (
            <NavLink key={to} to={to} end={end} onClick={() => setOpen(false)}>
              <Icon size={18} aria-hidden /> {label}
            </NavLink>
          ))}
        </nav>
        <button className="nav-logout" onClick={() => logout()}>
          <LogOut size={18} aria-hidden /> Keluar
        </button>
      </aside>
      {open && <div className="scrim only-mobile" onClick={() => setOpen(false)} />}
      <div className="main">
        <header className="topbar">
          <button className="icon-btn only-mobile" onClick={() => setOpen(true)} aria-label="Buka menu"><Menu /></button>
          <div className="topbar-title"><Avatar user={user} size={32} /> <span>Halo, <strong>{user?.username}</strong></span></div>
          <button className="icon-btn bell" onClick={() => nav("/notifikasi")} aria-label={`Notifikasi, ${unread} belum dibaca`}>
            <Bell size={20} />
            {unread > 0 && <span className="dot">{unread > 9 ? "9+" : unread}</span>}
          </button>
        </header>
        <main className="content">
          <Outlet context={{ refreshUnread: () => api.get("/me/notifications/unread-count").then((r) => setUnread(r.data?.unread ?? r.data?.count ?? 0)).catch(() => {}) }} />
        </main>
      </div>
    </div>
  );
}
