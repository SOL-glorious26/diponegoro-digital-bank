package com.diponegoro.ddb.demo;

import com.diponegoro.ddb.ledger.LedgerAccount;
import com.diponegoro.ddb.ledger.LedgerRepositories.AccountRepository;
import com.diponegoro.ddb.ledger.LedgerRepositories.JournalRepository;
import com.diponegoro.ddb.payment.PaymentService;
import com.diponegoro.ddb.support.ApiTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DemoDataTest extends ApiTestBase {
    @Autowired DemoDataSeeder seeder;
    @Autowired PlatformTransactionManager tm;
    @Autowired AccountRepository accounts;
    @Autowired JournalRepository journal;
    @Autowired PaymentService payments;

    @Test
    void dataDemoKonsistenDanBisaDipakai() throws Exception {
        new TransactionTemplate(tm).executeWithoutResult(s -> seeder.seed());

        // Invarian ledger: cache saldo setiap rekening = jumlah baris jurnalnya, dan total semua jurnal = 0.
        long total = 0;
        for (LedgerAccount a : accounts.findAll()) {
            long sum = journal.sumFor(a.id);
            assertThat(a.balance).as(a.number).isEqualTo(sum);
            total += sum;
        }
        assertThat(total).isZero();

        Res login = post("/api/v1/auth/login", null, Map.of("identifier", "demo", "password", "Demo@1234", "deviceId", "emu"));
        assertThat(login.status()).isEqualTo(200);
        String token = login.text("accessToken");
        Res acc = get("/api/v1/accounts/me", token);
        assertThat(acc.text("accountNumber")).isEqualTo("1400123456");
        assertThat(acc.num("balance")).isPositive();
        assertThat(get("/api/v1/history", token).num("totalItems")).isGreaterThan(30);

        // Transfer ke ANDI dan bayar QRIS pribadi ANDI dari daftar contoh
        Res t = pay("/api/v1/transfers", token, "demo-1", Map.of("bankCode", "DDB", "accountNumber", "1400000002",
                "amount", 25_000, "pin", "123456"));
        assertThat(t.text("toName")).isEqualTo("ANDI PRATAMA");
        String andiQr = payments.demoQrs().get(3).payload();
        assertThat(post("/api/v1/qris/scan", token, Map.of("payload", andiQr)).at("sameBank").asBoolean()).isTrue();
    }
}
