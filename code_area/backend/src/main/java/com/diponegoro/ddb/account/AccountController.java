package com.diponegoro.ddb.account;

import com.diponegoro.ddb.account.AccountDtos.AccountOverview;
import com.diponegoro.ddb.account.AccountDtos.BalanceChart;
import com.diponegoro.ddb.account.AccountDtos.BalancePeriod;
import com.diponegoro.ddb.account.AccountDtos.BalanceVisibilityRequest;
import com.diponegoro.ddb.account.AccountDtos.CreatePocketRequest;
import com.diponegoro.ddb.account.AccountDtos.LockPocketRequest;
import com.diponegoro.ddb.account.AccountDtos.MoveRequest;
import com.diponegoro.ddb.account.AccountDtos.PocketDto;
import com.diponegoro.ddb.account.AccountDtos.UpdatePocketRequest;
import com.diponegoro.ddb.account.AccountDtos.WithdrawRequest;
import com.diponegoro.ddb.common.Idempotency;
import com.diponegoro.ddb.security.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
@Tag(name = "2. Account", description = "FR-06 – FR-09: saldo, grafik saldo, kantong, locking")
public class AccountController {
    private final AccountService accounts;

    public AccountController(AccountService accounts) {
        this.accounts = accounts;
    }

    @GetMapping("/me")
    @Operation(summary = "Ringkasan rekening utama & saldo (FR-06)")
    public AccountOverview overview(@AuthenticationPrincipal AuthUser me) {
        return accounts.overview(me.userId());
    }

    @PutMapping("/me/balance-visibility")
    @Operation(summary = "Simpan pilihan sembunyikan/tampilkan saldo (FR-06)")
    public AccountOverview visibility(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody BalanceVisibilityRequest r) {
        accounts.setBalanceHidden(me.userId(), r.hidden());
        return accounts.overview(me.userId());
    }

    @GetMapping("/me/balance-chart")
    @Operation(summary = "Grafik saldo harian/mingguan/bulanan (FR-07)")
    public BalanceChart chart(@AuthenticationPrincipal AuthUser me,
                              @RequestParam(defaultValue = "DAILY") BalancePeriod period) {
        return accounts.balanceChart(me.userId(), period);
    }

    @GetMapping("/pockets")
    @Operation(summary = "Daftar kantong (FR-08)")
    public List<PocketDto> pockets(@AuthenticationPrincipal AuthUser me) {
        return accounts.pockets(me.userId());
    }

    @PostMapping("/pockets")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Buat kantong, opsional langsung dikunci sampai tanggal/target (FR-08, FR-09)")
    public PocketDto create(@AuthenticationPrincipal AuthUser me, @RequestBody CreatePocketRequest r) {
        return accounts.create(me.userId(), r);
    }

    @GetMapping("/pockets/{id}")
    public PocketDto pocket(@AuthenticationPrincipal AuthUser me, @PathVariable String id) {
        return accounts.pocket(me.userId(), id);
    }

    @PutMapping("/pockets/{id}")
    @Operation(summary = "Ubah nama, tujuan, target kantong")
    public PocketDto update(@AuthenticationPrincipal AuthUser me, @PathVariable String id, @RequestBody UpdatePocketRequest r) {
        return accounts.update(me.userId(), id, r);
    }

    @PostMapping("/pockets/{id}/lock")
    @Operation(summary = "Kunci kantong sampai tanggal atau target (FR-09)")
    public PocketDto lock(@AuthenticationPrincipal AuthUser me, @PathVariable String id, @Valid @RequestBody LockPocketRequest r) {
        return accounts.lock(me.userId(), id, r.lockType(), r.lockUntil());
    }

    @DeleteMapping("/pockets/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Hapus kantong; sisa saldo dipindah ke rekening utama")
    public void delete(@AuthenticationPrincipal AuthUser me, @PathVariable String id,
                       @RequestHeader(Idempotency.HEADER) String key) {
        accounts.delete(me.userId(), id, Idempotency.key(me.userId(), key));
    }

    @PostMapping("/pockets/{id}/deposit")
    @Operation(summary = "Isi kantong dari rekening utama (butuh header Idempotency-Key)")
    public PocketDto deposit(@AuthenticationPrincipal AuthUser me, @PathVariable String id,
                             @RequestHeader(Idempotency.HEADER) String key, @Valid @RequestBody MoveRequest r) {
        return accounts.deposit(me.userId(), id, r.amount(), Idempotency.key(me.userId(), key));
    }

    @PostMapping("/pockets/{id}/withdraw")
    @Operation(summary = "Tarik dari kantong; kantong terkunci butuh earlyWithdrawal=true + PIN (BR-06)")
    public PocketDto withdraw(@AuthenticationPrincipal AuthUser me, @PathVariable String id,
                              @RequestHeader(Idempotency.HEADER) String key, @Valid @RequestBody WithdrawRequest r) {
        return accounts.withdraw(me.userId(), id, r.amount(), Boolean.TRUE.equals(r.earlyWithdrawal()), r.pin(),
                Idempotency.key(me.userId(), key));
    }
}
