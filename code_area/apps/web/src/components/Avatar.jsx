import { useEffect, useState } from "react";

// Foto profil (FR-27) dengan cadangan inisial bila belum ada foto atau gambar gagal dimuat.
export default function Avatar({ user, size = 64 }) {
  const [broken, setBroken] = useState(false);
  useEffect(() => setBroken(false), [user?.photoUrl]);
  const initials = (user?.username || "?").slice(0, 2).toUpperCase();
  return user?.photoUrl && !broken
    ? <img className="avatar" src={user.photoUrl} alt={`Foto profil ${user.username}`} width={size} height={size}
        style={{ width: size, height: size }} onError={() => setBroken(true)} referrerPolicy="no-referrer" />
    : <span className="avatar avatar-initials" style={{ width: size, height: size, fontSize: size * 0.36 }} aria-hidden>{initials}</span>;
}
