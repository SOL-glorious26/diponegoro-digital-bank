import { useEffect, useRef } from "react";
import { AlertCircle, CheckCircle2, Info, Loader2, X } from "lucide-react";

export function Spinner({ label }) {
  return (
    <span className="spinner" role="status">
      <Loader2 size={18} className="spin" aria-hidden /> {label && <span>{label}</span>}
    </span>
  );
}

export function Button({ children, variant = "primary", loading, className = "", ...rest }) {
  return (
    <button className={`btn btn-${variant} ${className}`} disabled={loading || rest.disabled} {...rest}>
      {loading ? <Loader2 size={16} className="spin" aria-hidden /> : null}
      {children}
    </button>
  );
}

export function Field({ label, hint, error, children, id }) {
  return (
    <div className="field">
      {label && <label htmlFor={id}>{label}</label>}
      {children}
      {error ? <div className="field-error">{error}</div> : hint ? <div className="field-hint">{hint}</div> : null}
    </div>
  );
}

export function Alert({ kind = "error", children, onClose }) {
  if (!children) return null;
  const Icon = kind === "error" ? AlertCircle : kind === "success" ? CheckCircle2 : Info;
  return (
    <div className={`alert alert-${kind}`} role={kind === "error" ? "alert" : "status"}>
      <Icon size={18} aria-hidden />
      <div className="alert-body">{children}</div>
      {onClose && <button className="icon-btn" onClick={onClose} aria-label="Tutup"><X size={16} /></button>}
    </div>
  );
}

export function Card({ title, actions, children, className = "" }) {
  return (
    <section className={`card ${className}`}>
      {(title || actions) && (
        <header className="card-head">
          {title && <h2>{title}</h2>}
          {actions && <div className="card-actions">{actions}</div>}
        </header>
      )}
      {children}
    </section>
  );
}

export function Modal({ title, onClose, children, width = 460 }) {
  const ref = useRef(null);
  useEffect(() => {
    const onKey = (e) => e.key === "Escape" && onClose?.();
    document.addEventListener("keydown", onKey);
    ref.current?.querySelector("input,button")?.focus();
    return () => document.removeEventListener("keydown", onKey);
  }, [onClose]);
  return (
    <div className="modal-backdrop" onMouseDown={(e) => e.target === e.currentTarget && onClose?.()}>
      <div className="modal" role="dialog" aria-modal="true" aria-label={title} style={{ maxWidth: width }} ref={ref}>
        <header className="modal-head">
          <h2>{title}</h2>
          {onClose && <button className="icon-btn" onClick={onClose} aria-label="Tutup"><X size={18} /></button>}
        </header>
        {children}
      </div>
    </div>
  );
}

export function StatusBadge({ status, label }) {
  const cls = status === "SUCCESS" ? "ok" : status === "PROCESSING" ? "warn" : "bad";
  return <span className={`badge badge-${cls}`}>{label || status}</span>;
}

export function Rows({ items }) {
  return (
    <dl className="rows">
      {items.filter(Boolean).map(([k, v]) => (
        <div key={k}><dt>{k}</dt><dd>{v}</dd></div>
      ))}
    </dl>
  );
}

export function Empty({ children }) {
  return <div className="empty">{children}</div>;
}
