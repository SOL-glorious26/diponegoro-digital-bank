package com.diponegoro.ddb.transfer;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Simulator jaringan antarbank. Direktori rekening sama dengan aplikasi Android, termasuk akun
 * untuk skenario gagal: BRI 9999999999 = ditolak (A6), Mandiri 8888888888 = timeout (A5).
 */
@Component
public class InterbankSimulator implements InterbankGateway {

    private static final List<Bank> BANKS = List.of(
            new Bank(TransferService.INTERNAL_BANK, "Diponegoro Digital Bank"),
            new Bank("BCA", "BCA"),
            new Bank("BRI", "BRI"),
            new Bank("BNI", "BNI"),
            new Bank("MANDIRI", "Bank Mandiri"),
            new Bank("BSI", "Bank Syariah Indonesia"),
            new Bank("CIMB", "CIMB Niaga"),
            new Bank("PERMATA", "Bank Permata"),
            new Bank("BTN", "BTN"),
            new Bank("DANAMON", "Bank Danamon"));

    private record Entry(String name, Outcome outcome) {}

    private static final Map<String, Entry> DIRECTORY = Map.ofEntries(
            Map.entry("BCA:1234567890", new Entry("ANDI WIJAYA", Outcome.ACCEPTED)),
            Map.entry("BCA:1234567891", new Entry("IBU KOS MAWAR", Outcome.ACCEPTED)),
            Map.entry("BRI:9876543210", new Entry("BUDI HARTONO", Outcome.ACCEPTED)),
            Map.entry("BRI:0012345678", new Entry("ORANG TUA", Outcome.ACCEPTED)),
            Map.entry("BNI:1122334455", new Entry("CITRA AYU", Outcome.ACCEPTED)),
            Map.entry("MANDIRI:5566778899", new Entry("DIMAS ANGGARA", Outcome.ACCEPTED)),
            Map.entry("BSI:7001234567", new Entry("SITI AMINAH", Outcome.ACCEPTED)),
            Map.entry("CIMB:8001234567", new Entry("KEVIN SANJAYA", Outcome.ACCEPTED)),
            Map.entry("BRI:9999999999", new Entry("RINA (SIMULASI DITOLAK)", Outcome.REJECTED)),
            Map.entry("MANDIRI:8888888888", new Entry("TONI (SIMULASI TIMEOUT)", Outcome.TIMEOUT)));

    @Override
    public List<Bank> banks() {
        return BANKS;
    }

    @Override
    public Optional<Bank> bank(String code) {
        return BANKS.stream().filter(b -> b.code().equals(code)).findFirst();
    }

    @Override
    public Optional<String> inquiry(String bankCode, String accountNumber) {
        return Optional.ofNullable(DIRECTORY.get(bankCode + ":" + accountNumber)).map(Entry::name);
    }

    @Override
    public Outcome send(String bankCode, String accountNumber, long amount, String reference) {
        Entry e = DIRECTORY.get(bankCode + ":" + accountNumber);
        return e == null ? Outcome.REJECTED : e.outcome();
    }

    /** Pada simulator, transaksi yang timeout dianggap sudah sampai saat dicek ulang. */
    @Override
    public Outcome status(String reference) {
        return Outcome.ACCEPTED;
    }
}
