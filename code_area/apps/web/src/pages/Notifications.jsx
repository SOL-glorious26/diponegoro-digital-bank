import { useCallback, useEffect, useState } from "react";
import { useOutletContext } from "react-router-dom";
import { ArrowDownLeft, ArrowUpRight, Info, ShieldAlert } from "lucide-react";
import { api, errorMessage } from "../api/client";
import { dateTime } from "../lib/format";
import { Alert, Button, Card, Empty, Spinner } from "../components/ui";
import { TxDetailModal } from "../components/TxDetail";

const ICON = { INCOMING: ArrowDownLeft, OUTGOING: ArrowUpRight, SECURITY: ShieldAlert, INFO: Info };

// Kotak notifikasi (FR-29).
export default function Notifications() {
  const { refreshUnread } = useOutletContext() || {};
  const [items, setItems] = useState(null);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(false);
  const [err, setErr] = useState(null);
  const [detail, setDetail] = useState(null);

  const load = useCallback(async (p) => {
    try {
      const { data } = await api.get("/me/notifications", { params: { page: p } });
      setItems((prev) => (p === 0 ? data.items : [...(prev || []), ...data.items]));
      setHasMore(data.hasMore); setPage(p);
    } catch (ex) { setErr(errorMessage(ex)); }
  }, []);
  useEffect(() => { load(0); }, [load]);

  async function open(n) {
    if (!n.read) {
      await api.post(`/me/notifications/${n.id}/read`).catch(() => {});
      setItems((list) => list.map((x) => (x.id === n.id ? { ...x, read: true } : x)));
      refreshUnread?.();
    }
    if (n.transactionId) setDetail(n.transactionId);
  }
  async function readAll() {
    await api.post("/me/notifications/read-all").catch(() => {});
    setItems((list) => list?.map((x) => ({ ...x, read: true })));
    refreshUnread?.();
  }

  return (
    <div className="page narrow">
      <Card title="Notifikasi" actions={<Button variant="secondary" onClick={readAll}>Tandai semua dibaca</Button>}>
        <Alert>{err}</Alert>
        {items == null ? <Spinner /> : items.length === 0 ? <Empty>Belum ada notifikasi.</Empty> : (
          <ul className="notif-list">
            {items.map((n) => {
              const Icon = ICON[n.type] || Info;
              return (
                <li key={n.id}>
                  <button className={`notif ${n.read ? "" : "unread"}`} onClick={() => open(n)}>
                    <span className={`tx-icon notif-${(n.type || "").toLowerCase()}`}><Icon size={18} aria-hidden /></span>
                    <span className="notif-body">
                      <strong>{n.title}</strong>
                      <span>{n.message}</span>
                      <span className="small muted">{dateTime(n.createdAt)}</span>
                    </span>
                  </button>
                </li>
              );
            })}
          </ul>
        )}
        {hasMore && <div className="center"><Button variant="secondary" onClick={() => load(page + 1)}>Muat lebih banyak</Button></div>}
      </Card>
      {detail && <TxDetailModal txId={detail} onClose={() => setDetail(null)} />}
    </div>
  );
}
