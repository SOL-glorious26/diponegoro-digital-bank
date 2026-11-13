package com.diponegoro.ddb.payment;

import com.diponegoro.ddb.payment.QrisCodec.Payload;
import com.diponegoro.ddb.support.ApiTestBase;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentTopUpCardFlowTest extends ApiTestBase {

    private static final String WARUNG = QrisCodec.encode(new Payload("ID.CO.QRIS.WWW", "9360000M0001", "M0001",
            "WARUNG BU SRI", "SEMARANG", "5812", null));
    private static final String KOPI = QrisCodec.encode(new Payload("ID.CO.QRIS.WWW", "9360000M0002", "M0002",
            "KOPI SENJA TEMBALANG", "SEMARANG", "5814", 28_000L));

    // ================================================================== QRIS

    @Test
    void codecRoundTripDanCrcRusakDitolak() throws Exception {
        Payload p = QrisCodec.decode(KOPI);
        assertThat(p.merchantName()).isEqualTo("KOPI SENJA TEMBALANG");
        assertThat(p.amount()).isEqualTo(28_000L);
        Customer c = newCustomer();
        String rusak = KOPI.substring(0, KOPI.length() - 1) + (KOPI.endsWith("0") ? "1" : "0");
        assertThat(post("/api/v1/qris/scan", c.token(), Map.of("payload", rusak)).text("message"))
                .isEqualTo("Kode QRIS rusak (CRC tidak cocok)");
    }

    @Test
    void bayarQrisStatisDanBatasNominal() throws Exception {
        Customer c = newCustomer();
        Res scan = post("/api/v1/qris/scan", c.token(), Map.of("payload", WARUNG));
        assertThat(scan.text("merchantName")).isEqualTo("WARUNG BU SRI");
        assertThat(scan.at("dynamic").asBoolean()).isFalse();

        assertThat(pay("/api/v1/qris/pay", c.token(), "q0", Map.of("payload", WARUNG, "amount", 500, "pin", PIN)).text("message"))
                .isEqualTo("Nominal QRIS Rp1.000 – Rp10.000.000");
        Res ok = pay("/api/v1/qris/pay", c.token(), "q1", Map.of("payload", WARUNG, "amount", 25_000, "pin", PIN));
        assertThat(ok.text("status")).isEqualTo("SUCCESS");
        assertThat(ok.text("toName")).isEqualTo("WARUNG BU SRI");
        assertThat(balance(c)).isEqualTo(4_975_000);
    }

    @Test
    void qrisDinamisMemakaiNominalDariKode() throws Exception {
        Customer c = newCustomer();
        Res ok = pay("/api/v1/qris/pay", c.token(), "q", Map.of("payload", KOPI, "amount", 1, "pin", PIN));
        assertThat(ok.num("amount")).isEqualTo(28_000);
        assertThat(balance(c)).isEqualTo(4_972_000);
    }

    @Test
    void qrisPribadiSesamaBankMengkreditPenerimaDanMenolakQrSendiri() throws Exception {
        Customer a = newCustomer();
        Customer b = newCustomer();
        Res qr = get("/api/v1/qris/my-qr?amount=75000", b.token());
        assertThat(qr.at("dynamic").asBoolean()).isTrue();
        assertThat(post("/api/v1/qris/scan", b.token(), Map.of("payload", qr.text("payload"))).text("code")).isEqualTo("OWN_QRIS");

        Res paid = pay("/api/v1/qris/pay", a.token(), "p", Map.of("payload", qr.text("payload"), "pin", PIN));
        assertThat(paid.text("status")).isEqualTo("SUCCESS");
        assertThat(balance(a)).isEqualTo(4_925_000);
        assertThat(balance(b)).isEqualTo(5_075_000);
    }

    @Test
    void simulasiDanaMasukQris() throws Exception {
        Customer c = newCustomer();
        Res r = pay("/api/v1/qris/simulate-incoming", c.token(), "in", Map.of("amount", 75_000, "payerName", "Rudi"));
        assertThat(r.text("direction")).isEqualTo("IN");
        assertThat(r.text("fromName")).isEqualTo("RUDI");
        assertThat(balance(c)).isEqualTo(5_075_000);
    }

    // ================================================================== Top-up

    @Test
    void tokenPlnDanIdTidakDitemukan() throws Exception {
        Customer c = newCustomer();
        assertThat(get("/api/v1/topup/pln/inquiry?meterNumber=123456780000", c.token()).text("message"))
                .isEqualTo("ID pelanggan tidak ditemukan");
        Res cust = get("/api/v1/topup/pln/inquiry?meterNumber=123456789012", c.token());
        assertThat(cust.status()).isEqualTo(200);

        Map<String, Object> order = Map.of("kind", "PLN", "meterNumber", "123456789012", "nominal", 50_000, "pin", PIN);
        Res r = pay("/api/v1/topup/orders", c.token(), "pln", order);
        assertThat(r.status()).isEqualTo(200);
        assertThat(r.num("total")).isEqualTo(52_500);
        assertThat(r.at("extra").get("Token").asString()).matches("\\d{4}-\\d{4}-\\d{4}-\\d{4}-\\d{4}");
        assertThat(r.text("toName")).isEqualTo(cust.text("name"));
        assertThat(balance(c)).isEqualTo(4_947_500);
    }

    @Test
    void pulsaDanNominalTidakValid() throws Exception {
        Customer c = newCustomer();
        Res bad = pay("/api/v1/topup/orders", c.token(), "x", Map.of("kind", "PULSA", "phone", "081234567890", "nominal", 7_000, "pin", PIN));
        assertThat(bad.text("message")).isEqualTo("Pilih nominal pulsa");
        Res ok = pay("/api/v1/topup/orders", c.token(), "y", Map.of("kind", "PULSA", "phone", "081234567890", "nominal", 10_000, "pin", PIN));
        assertThat(ok.text("title")).isEqualTo("Pulsa Telkomsel Rp10.000");
        assertThat(ok.num("fee")).isEqualTo(1_500);
    }

    @Test
    void tagihanTidakBisaDibayarDuaKali() throws Exception {
        Customer c = newCustomer();
        Res inq = get("/api/v1/topup/bills/inquiry?biller=PDAM&customerNumber=123456789", c.token());
        assertThat(inq.status()).isEqualTo(200);
        Map<String, Object> order = new HashMap<>(Map.of("kind", "BILL", "biller", "PDAM", "customerNumber", "123456789", "pin", PIN));
        assertThat(pay("/api/v1/topup/orders", c.token(), "b1", order).text("status")).isEqualTo("SUCCESS");
        assertThat(pay("/api/v1/topup/orders", c.token(), "b2", order).text("code")).isEqualTo("BILL_PAID");
        assertThat(get("/api/v1/topup/bills/inquiry?biller=PDAM&customerNumber=123456789", c.token()).status()).isEqualTo(409);
    }

    // ================================================================== Virtual Card

    @Test
    void kartuVirtualDetailBerPinBekukanLimitDanCakupan() throws Exception {
        Customer c = newCustomer();
        Res card = post("/api/v1/cards", c.token(), Map.of("usageScope", "DOMESTIC"));
        assertThat(card.status()).isEqualTo(201);
        assertThat(card.text("maskedNumber")).startsWith("•••• •••• •••• ");
        assertThat(card.at("cvv").isMissingNode()).isTrue();
        String id = card.text("id");

        assertThat(post("/api/v1/cards/" + id + "/details", c.token(), Map.of("pin", "000000")).status()).isEqualTo(422);
        Res det = post("/api/v1/cards/" + id + "/details", c.token(), Map.of("pin", PIN));
        assertThat(det.text("cardNumber")).hasSize(16).endsWith(card.text("last4"));
        assertThat(det.text("cvv")).matches("\\d{3}");

        String url = "/api/v1/cards/" + id + "/simulate-purchase";
        post("/api/v1/cards/" + id + "/freeze", c.token(), null);
        assertThat(pay(url, c.token(), "c1", Map.of("amount", 50_000)).text("code")).isEqualTo("CARD_FROZEN");
        post("/api/v1/cards/" + id + "/activate", c.token(), null);
        assertThat(pay(url, c.token(), "c2", Map.of("amount", 50_000)).text("status")).isEqualTo("SUCCESS");

        assertThat(put("/api/v1/cards/" + id + "/limit", c.token(), Map.of("limit", 5_000)).text("message"))
                .isEqualTo("Limit minimal Rp10.000");
        put("/api/v1/cards/" + id + "/limit", c.token(), Map.of("limit", 100_000));
        assertThat(pay(url, c.token(), "c3", Map.of("amount", 150_000)).text("message"))
                .isEqualTo("Transaksi ditolak: melebihi limit Rp100.000");
        assertThat(pay(url, c.token(), "c4", Map.of("amount", 10_000, "international", true)).text("code"))
                .isEqualTo("CARD_DOMESTIC_ONLY");
        assertThat(balance(c)).isEqualTo(4_950_000);
    }
}
