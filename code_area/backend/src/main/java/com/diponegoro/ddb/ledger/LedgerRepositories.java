package com.diponegoro.ddb.ledger;

import com.diponegoro.ddb.ledger.LedgerModels.AccountKind;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public final class LedgerRepositories {
    private LedgerRepositories() {}

    public interface AccountRepository extends JpaRepository<LedgerAccount, String> {
        Optional<LedgerAccount> findFirstByOwnerUserIdAndKind(String ownerUserId, AccountKind kind);

        List<LedgerAccount> findByOwnerUserIdAndKindAndClosedFalseOrderByCreatedAt(String ownerUserId, AccountKind kind);

        Optional<LedgerAccount> findByNumberAndKindAndClosedFalse(String number, AccountKind kind);

        boolean existsByNumber(String number);
    }

    public interface TransactionRepository extends JpaRepository<LedgerTransaction, String>,
            JpaSpecificationExecutor<LedgerTransaction> {
        Optional<LedgerTransaction> findByIdempotencyKey(String key);

        Optional<LedgerTransaction> findByReference(String reference);

        @Query("select t from LedgerTransaction t where t.debitUserId = :userId or t.creditUserId = :userId order by t.createdAt desc")
        Page<LedgerTransaction> findForUser(String userId, Pageable pageable);
    }

    public interface JournalRepository extends JpaRepository<JournalLine, Long> {
        List<JournalLine> findByTxIdOrderById(String txId);

        @Query("select coalesce(sum(j.amount), 0) from JournalLine j where j.accountId = :accountId and j.postedAt <= :at")
        long balanceAt(String accountId, Instant at);

        @Query("select coalesce(sum(j.amount), 0) from JournalLine j where j.accountId = :accountId")
        long sumFor(String accountId);
    }
}
