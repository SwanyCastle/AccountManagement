package com.account.service;

import com.account.domain.Account;
import com.account.domain.AccountUser;
import com.account.domain.Transaction;
import com.account.dto.TransactionDto;
import com.account.exception.AccountException;
import com.account.repository.AccountRepository;
import com.account.repository.AccountUserRepository;
import com.account.repository.TransactionRepository;
import com.account.type.AccountStatus;
import com.account.type.ErrorCode;
import com.account.type.TransactionResultType;
import com.account.type.TransactionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import static com.account.type.TransactionResultType.F;
import static com.account.type.TransactionResultType.S;
import static com.account.type.TransactionType.CANCEL;
import static com.account.type.TransactionType.USE;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final AccountUserRepository accountUserRepository;
    private final AccountRepository accountRepository;

    // Service 에 있는 코드들은 @Transactional 을 달아주는게 약간 관례 느낌
    // 처음에는 한건의 처리만한다고 해도 나중에 코드 수정으로 로직이 복잡해지거나
    // 여러 처리를 진행해야 하는 경우도 있기에
    // 그래서 @Transactional 을 아예 class 위에다가 다는 방법도 있다.
    // 이렇게 되면 모든 public 메서드에 @Transactional 이 자동으로 적용이 된다.
    @Transactional
    public TransactionDto useBalance(Long userId, String accountNumber, Long amount) {
        AccountUser accountUser = accountUserRepository.findById(userId)
                .orElseThrow(() -> new AccountException(ErrorCode.USER_NOT_FOUND));

        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountException(ErrorCode.ACCOUNT_NOT_FOUND));

        validateUseBalance(accountUser, account, amount);

        // 이 함수에서 두가지 작업을 한다.
        // 이때 @Transactional 애노테이션이 붙어있어서
        // 두 개다 성공하거나, 두 개다 실패한다.

        // 만약 1번 처리에서 잔액이 변경이 되었는데
        // 2번 처리에서 무엇인가 문제가 생겨서 실행되지 않는다면
        // 1번 처리도 처리되지 않고 롤백이된다.
        // (애초에 1번 처리에서 잔액이 변경된다고 해서 바로 저장하지 않는다)

        // 1. account table 에 있는 해당 계좌의 balance 를 변경
        account.useBalance(amount);

        // 2. trasaction table 에 새로운 data 생성
        return TransactionDto.fromEntity(saveAndGetTransaction(USE, S, account, amount));
    }

    private void validateUseBalance(AccountUser accountUser, Account account, Long amount) {
        if (!Objects.equals(accountUser.getId(), account.getAccountUser().getId())) {
            throw new AccountException(ErrorCode.USER_ACCOUNT_UN_MATCH);
        }

        if (account.getAccountStatus() != AccountStatus.IN_USE) {
            throw new AccountException(ErrorCode.ACCOUNT_ALREADY_UNREGISTERED);
        }

        if (account.getBalance() < amount) {
            throw new AccountException(ErrorCode.AMOUNT_EXCEED_BALANCE);
        }
    }

    @Transactional
    public void saveFailedUseTransaction(String accountNumber, Long amount) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountException(ErrorCode.ACCOUNT_NOT_FOUND));
        saveAndGetTransaction(USE, F, account, amount);
    }

    private Transaction saveAndGetTransaction(
            TransactionType transactionType,
            TransactionResultType transactionResultType,
            Account account,
            Long amount
    ) {
        return transactionRepository.save(
                Transaction.builder()
                        .transactionType(transactionType)
                        .transactionResultType(transactionResultType)
                        .account(account)
                        .amount(amount)
                        .balanceSnapshot(account.getBalance())
                        .transactionId(
                                UUID.randomUUID()
                                        .toString()
                                        .replace("-", "")
                        )
                        .transactedAt(LocalDateTime.now())
                        .build()
        );
    }

    @Transactional
    public TransactionDto cancelBalance(
            String transactionId, String accountNumber, Long amount
    ) {
        Transaction transaction = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new AccountException(ErrorCode.TRANSACTION_NOT_FOUND));
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountException(ErrorCode.ACCOUNT_NOT_FOUND));

        validateCancelBalance(transaction, account, amount);

        account.cancelBalance(amount);

        return TransactionDto.fromEntity(saveAndGetTransaction(CANCEL, S, account, amount));
    }

    private void validateCancelBalance(Transaction transaction, Account account, Long amount) {
        if (!Objects.equals(transaction.getAccount().getId(), account.getId())) {
            throw new AccountException(ErrorCode.TRANSACTION_ACCOUNT_UN_MATCH);
        }

        if (!Objects.equals(transaction.getAmount(), amount)) {
            throw new AccountException(ErrorCode.CANCEL_MUST_FULLY);
        }

        if (transaction.getTransactedAt().isBefore(LocalDateTime.now().minusYears(1))) {
            throw new AccountException(ErrorCode.TOO_OLD_ORDER_TO_CANCEL);
        }
    }

    @Transactional
    public void saveFailedCancelTransaction(String accountNumber, Long amount) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountException(ErrorCode.ACCOUNT_NOT_FOUND));
        saveAndGetTransaction(CANCEL, F, account, amount);
    }

    public TransactionDto queryTranscation(String transactionId) {
        return TransactionDto.fromEntity(transactionRepository
                .findByTransactionId(transactionId)
                .orElseThrow(
                        () -> new AccountException(ErrorCode.TRANSACTION_NOT_FOUND)
                )
        );
    }
}
