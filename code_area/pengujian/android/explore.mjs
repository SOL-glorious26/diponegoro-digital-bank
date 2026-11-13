// Alat bantu eksplorasi: jalankan urutan langkah Android dan cetak teks layar.
// Pemakaian: node android/explore.mjs "tap:Transfer" "type:Nomor rekening=1400000002" "back" "print" "shot:nama"
import * as A from "../lib/adbui.mjs";
const esc = (s) => s.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
for (const step of process.argv.slice(2)) {
  const [cmd, ...restParts] = step.split(":");
  const rest = restParts.join(":");
  if (cmd === "tap") await A.tap(new RegExp("^" + esc(rest) + "$"));
  else if (cmd === "tapre") await A.tap(new RegExp(rest));
  else if (cmd === "type") { const [label, val] = rest.split("="); await A.typeInto(new RegExp("^" + esc(label) + "$"), val); }
  else if (cmd === "back") { A.back(); await A.sleep(700); }
  else if (cmd === "up") { A.swipeUp(); await A.sleep(700); }
  else if (cmd === "wait") await A.sleep(+rest || 1500);
  else if (cmd === "print") console.log(">", A.screenText());
  else if (cmd === "fields") console.log("fields:", A.dump().filter((n) => n.editable).map((n) => JSON.stringify(n.text)).join(" | "));
  else if (cmd === "shot") A.screenshot(`hasil/${rest}.png`);
  await A.sleep(400);
}
