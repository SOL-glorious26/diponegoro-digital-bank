import { useEffect, useState } from "react";
import { ArrowLeft, Printer } from "lucide-react";
import { api, errorMessage, newIdempotencyKey, withKey } from "../api/client";
import { formatAmountInput, onlyDigits, parseAmount, rupiah } from "../lib/format";
import { Alert, Button, Card, Field, Rows, Spinner } from "../components/ui";
import PinDialog from "../components/PinDialog";
import { Receipt } from "../components/TxDetail";

const TABS = [["PULSA", "Pulsa"], ["DATA", "Paket data"], ["PLN", "Token PLN"], ["BILL", "Tagihan"], ["EWALLET", "E-wallet"]];

export default function TopUp() {
  const [catalog, setCatalog] = useState(null);
  const [kind, setKind] = useState("PULSA");
  const [order, setOrder] = useState(null); // {request, quote}
  const [key, setKey] = useState(null);
  const [pinOpen, setPinOpen] = useState(false);
  const [result, setResult] = useState(null);
  const [err, setErr] = useState(null);

  useEffect(() => { api.get("/topup/catalog").then((r) => setCatalog(r.data)).catch((ex) => setErr(errorMessage(ex))); }, []);

  async function toConfirm(request) {
    setErr(null);
    const { data } = await api.post("/topup/quote", request);
    setOrder({ request, quote: data });
    setKey(newIdempotencyKey());
  }

  async function execute(pin) {
    const { data } = await api.post("/topup/orders", { ...order.request, pin }, withKey(key));
    setPinOpen(false);
    setResult(data);
  }

  const reset = () => { setOrder(null); setResult(null); setKey(null); setErr(null); };

  if (result) {
    return (
      <div className="page narrow">
        <Card title="Bukti pembayaran">
          <Receipt tx={result} />
          <div className="row-actions no-print">
            <Button variant="secondary" onClick={() => window.print()}><Printer size={16} aria-hidden /> Cetak / PDF</Button>
            <Button onClick={reset}>Selesai</Button>
          </div>
        </Card>
      </div>
    );
  }

  if (order) {
    const q = order.quote;
    const details = q.details && typeof q.details === "object" ? Object.entries(q.details).filter(([, v]) => v != null && typeof v !== "object") : [];
    return (
      <div className="page narrow">
        <Card title="Konfirmasi pembayaran" actions={<button className="link" onClick={() => setOrder(null)}><ArrowLeft size={14} aria-hidden /> Ubah</button>}>
          <Rows items={[
            ["Produk", q.title],
            ["Tujuan", q.target],
            q.targetName && ["Nama", q.targetName],
            ...details.map(([k, v]) => [k.replace(/([A-Z])/g, " $1").replace(/^./, (c) => c.toUpperCase()), String(v)]),
            ["Harga", rupiah(q.amount)],
            ["Biaya admin", rupiah(q.fee)],
            ["Total", <strong key="t">{rupiah(q.total)}</strong>],
          ]} />
          <Button className="block" onClick={() => setPinOpen(true)}>Bayar dengan PIN</Button>
        </Card>
        {pinOpen && <PinDialog summary={<p className="center">{q.title} · {rupiah(q.total)}</p>} onSubmit={execute} onClose={() => setPinOpen(false)} />}
      </div>
    );
  }

  return (
    <div className="page narrow">
      <Card title="Top-up & tagihan">
        <div className="tabs" role="tablist">
          {TABS.map(([k, l]) => (
            <button key={k} role="tab" aria-selected={kind === k} className={kind === k ? "on" : ""} onClick={() => { setKind(k); setErr(null); }}>{l}</button>
          ))}
        </div>
        {!catalog ? <Spinner label="Memuat katalog…" /> : (
          <OrderForm key={kind} kind={kind} catalog={catalog} onSubmit={toConfirm} />
        )}
        <Alert>{err}</Alert>
      </Card>
    </div>
  );
}

function OrderForm({ kind, catalog, onSubmit }) {
  const [phone, setPhone] = useState("");
  const [nominal, setNominal] = useState(null);
  const [packages, setPackages] = useState(null);
  const [productId, setProductId] = useState(null);
  const [meter, setMeter] = useState("");
  const [pln, setPln] = useState(null);
  const [biller, setBiller] = useState(catalog.billers?.[0]?.code ?? "");
  const [customer, setCustomer] = useState("");
  const [bill, setBill] = useState(null);
  const [wallet, setWallet] = useState(catalog.ewallets?.[0]?.code ?? "");
  const [amount, setAmount] = useState("");
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState(null);

  const operator = catalog.operators?.find((o) => o.prefixes?.some((p) => phone.startsWith(p)));
  const run = async (fn) => { setBusy(true); setErr(null); try { await fn(); } catch (ex) { setErr(errorMessage(ex)); } finally { setBusy(false); } };

  const submit = (e) => {
    e.preventDefault();
    run(async () => {
      if (kind === "PULSA") {
        if (!nominal) throw { response: { data: { message: "Pilih nominal pulsa." } } };
        await onSubmit({ kind, phone, nominal });
      } else if (kind === "DATA") {
        if (!productId) throw { response: { data: { message: "Pilih paket data." } } };
        await onSubmit({ kind, phone, productId });
      } else if (kind === "PLN") {
        if (!pln || !nominal) throw { response: { data: { message: "Cek ID pelanggan lalu pilih nominal." } } };
        await onSubmit({ kind, meterNumber: meter, nominal });
      } else if (kind === "BILL") {
        if (!bill) throw { response: { data: { message: "Cek tagihan terlebih dahulu." } } };
        await onSubmit({ kind, biller, customerNumber: customer });
      } else {
        await onSubmit({ kind, wallet, phone, amount: parseAmount(amount) });
      }
    });
  };

  const chips = (values, current, setter, fee) => (
    <div className="chips">
      {values?.map((v) => (
        <button type="button" key={v} className={current === v ? "chip on" : "chip"} onClick={() => setter(v)}>
          {rupiah(v)}
        </button>
      ))}
      {fee != null && <span className="small muted">Biaya admin {rupiah(fee)}</span>}
    </div>
  );

  return (
    <form onSubmit={submit}>
      {(kind === "PULSA" || kind === "DATA" || kind === "EWALLET") && (
        <Field label="Nomor HP" id="ph" hint={kind !== "EWALLET" ? (operator ? `Operator: ${operator.name}` : "Operator dideteksi dari awalan nomor") : null}>
          <input id="ph" inputMode="tel" value={phone} onChange={(e) => { setPhone(onlyDigits(e.target.value)); setPackages(null); setProductId(null); }} required placeholder="0812…" />
        </Field>
      )}
      {kind === "PULSA" && <Field label="Nominal">{chips(catalog.pulsaNominals, nominal, setNominal, catalog.pulsaFee)}</Field>}
      {kind === "DATA" && (
        <>
          {!packages && <Button type="button" variant="secondary" loading={busy} disabled={phone.length < 10}
            onClick={() => run(async () => setPackages((await api.get("/topup/data-packages", { params: { phone } })).data))}>Lihat paket</Button>}
          {packages && (
            <div className="options">
              <p className="small muted">Operator {packages.operator?.name}</p>
              {packages.packages?.map((p) => (
                <label key={p.id} className={`option ${productId === p.id ? "on" : ""}`}>
                  <input type="radio" name="pkg" checked={productId === p.id} onChange={() => setProductId(p.id)} />
                  <span><strong>{p.name}</strong><br /><span className="small muted">{p.description}</span></span>
                  <span>{rupiah(p.price)}</span>
                </label>
              ))}
            </div>
          )}
        </>
      )}
      {kind === "PLN" && (
        <>
          <Field label="ID pelanggan / nomor meter" id="mt" hint="Simulasi: ID berakhiran 0000 tidak ditemukan">
            <div className="input-btn">
              <input id="mt" inputMode="numeric" value={meter} onChange={(e) => { setMeter(onlyDigits(e.target.value)); setPln(null); }} required />
              <Button type="button" variant="secondary" loading={busy && !pln} onClick={() => run(async () => setPln((await api.get("/topup/pln/inquiry", { params: { meterNumber: meter } })).data))}>Cek</Button>
            </div>
          </Field>
          {pln && <Alert kind="success">{pln.name} · tarif {pln.tariff}</Alert>}
          {pln && <Field label="Nominal token">{chips(catalog.plnNominals, nominal, setNominal, catalog.plnFee)}</Field>}
        </>
      )}
      {kind === "BILL" && (
        <>
          <Field label="Jenis tagihan" id="bl">
            <select id="bl" value={biller} onChange={(e) => { setBiller(e.target.value); setBill(null); }}>
              {catalog.billers?.map((b) => <option key={b.code} value={b.code}>{b.label}</option>)}
            </select>
          </Field>
          <Field label="Nomor pelanggan" id="cn">
            <div className="input-btn">
              <input id="cn" inputMode="numeric" value={customer} onChange={(e) => { setCustomer(onlyDigits(e.target.value)); setBill(null); }} required />
              <Button type="button" variant="secondary" loading={busy && !bill} onClick={() => run(async () => setBill((await api.get("/topup/bills/inquiry", { params: { biller, customerNumber: customer } })).data))}>Cek tagihan</Button>
            </div>
          </Field>
          {bill && <Alert kind="success">{bill.customerName} · {bill.billerLabel} {bill.period}: {rupiah(bill.amount)} (+ biaya {rupiah(bill.fee)})</Alert>}
        </>
      )}
      {kind === "EWALLET" && (
        <>
          <Field label="E-wallet" id="ew">
            <select id="ew" value={wallet} onChange={(e) => setWallet(e.target.value)}>
              {catalog.ewallets?.map((w) => <option key={w.code} value={w.code}>{w.label}</option>)}
            </select>
          </Field>
          <Field label="Nominal" id="ea" hint={`${rupiah(catalog.ewalletMin)} – ${rupiah(catalog.ewalletMax)}; biaya ${rupiah(catalog.ewalletFee)}`}>
            <div className="input-prefix"><span>Rp</span><input id="ea" inputMode="numeric" value={amount} onChange={(e) => setAmount(formatAmountInput(e.target.value))} required /></div>
          </Field>
        </>
      )}
      <Alert>{err}</Alert>
      <Button type="submit" loading={busy} className="block">Lanjut</Button>
    </form>
  );
}
