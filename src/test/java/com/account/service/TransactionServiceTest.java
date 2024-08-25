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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.account.type.TransactionResultType.F;
import static com.account.type.TransactionResultType.S;
import static com.account.type.TransactionType.CANCEL;
import static com.account.type.TransactionType.USE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {
    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountUserRepository accountUserRepository;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    void successUseBalance() {
        // given
        AccountUser user = AccountUser.builder().name("Pobi").build();
        user.setId(12L);
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));

        Account account = Account.builder()
                .accountUser(user)
                .accountStatus(AccountStatus.IN_USE)
                .balance(10000L)
                .accountNumber("1000000012").build();

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));

        given(transactionRepository.save(any()))
                .willReturn(Transaction.builder()
                        .account(account)
                        .transactionType(USE)
                        .transactionResultType(S)
                        .amount(1000L)
                        .balanceSnapshot(9000L)
                        .transactionId("transactionId")
                        .transactedAt(LocalDateTime.now())
                        .build()
                );

        ArgumentCaptor<Transaction> captor =
                ArgumentCaptor.forClass(Transaction.class);

        // when
        TransactionDto transactionDto = transactionService.useBalance(
                1L, "10000000000", 200L
        );

        // then
        verify(transactionRepository, times(1))
                .save(captor.capture());
        assertEquals(S, captor.getValue().getTransactionResultType());
        assertEquals(USE, captor.getValue().getTransactionType());
        assertEquals(9800L, captor.getValue().getBalanceSnapshot());
        assertEquals(200L, captor.getValue().getAmount());
        assertEquals(S, transactionDto.getTransactionResultType());
        assertEquals(USE, transactionDto.getTransactionType());
        assertEquals(9000L, transactionDto.getBalanceSnapshot());
        assertEquals(1000L, transactionDto.getAmount());
    }

    @Test
    @DisplayName("해당 유저 없음 - 잔액 사용 실패")
    void useBalance_UserNotFound() {
        // given
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.empty());

        // when
        AccountException accountException = assertThrows(AccountException.class,
                () -> transactionService.useBalance(
                        1L, "10000000000", 200L
                ));

        // then
        assertEquals(ErrorCode.USER_NOT_FOUND, accountException.getErrorCode());
    }

    @Test
    @DisplayName("해당 계좌 없음 - 잔액 사용 실패")
    void useBalance_AccountNotFound() {
        AccountUser user = AccountUser.builder().name("Pobi").build();
        user.setId(12L);

        // given
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.empty());

        // when
        AccountException accountException = assertThrows(AccountException.class,
                () -> transactionService.useBalance(
                        1L, "10000000000", 200L
                ));

        // then
        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, accountException.getErrorCode());
    }

    @Test
    @DisplayName("계좌 소유주 다름 - 잔액 사용 실패")
    void useBalance_userUnMatch() {
        // given
        AccountUser pobi = AccountUser.builder().name("Pobi").build();
        pobi.setId(12L);
        AccountUser harry = AccountUser.builder().name("Harry").build();
        harry.setId(13L);

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(pobi));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(harry)
                        .balance(0L)
                        .accountNumber("1000000012").build()));

        // when
        AccountException accountException = assertThrows(AccountException.class,
                () -> transactionService.useBalance(
                        1L, "10000000000", 200L
                ));

        // then
        assertEquals(ErrorCode.USER_ACCOUNT_UN_MATCH, accountException.getErrorCode());
    }

    @Test
    @DisplayName("이미 해지된 계좌 - 잔액 사용 실패")
    void useBalance_alreadyUnregistered() {
        // given
        AccountUser pobi = AccountUser.builder().name("Pobi").build();
        pobi.setId(12L);

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(pobi));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(pobi)
                        .accountStatus(AccountStatus.UNREGISTERED)
                        .balance(0L)
                        .accountNumber("1000000012").build()));

        // when
        AccountException accountException = assertThrows(AccountException.class,
                () -> transactionService.useBalance(
                        1L, "10000000000", 200L
                ));


        // then
        assertEquals(ErrorCode.ACCOUNT_ALREADY_UNREGISTERED, accountException.getErrorCode());
    }

    @Test
    @DisplayName("거래금액 > 잔액 - 잔액 사용 실패")
    void exceedAmount_UseBalance() {
        // given
        AccountUser user = AccountUser.builder().name("Pobi").build();
        user.setId(12L);

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));

        Account account = Account.builder()
                .accountUser(user)
                .accountStatus(AccountStatus.IN_USE)
                .balance(100L)
                .accountNumber("1000000012").build();

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));

        // when
        AccountException accountException = assertThrows(AccountException.class,
                () -> transactionService.useBalance(
                        1L, "10000000000", 200L
                ));

        // then
        assertEquals(ErrorCode.AMOUNT_EXCEED_BALANCE, accountException.getErrorCode());
    }

    @Test
    @DisplayName("트랜잭션 실패시 응답 및 데이터 테스트")
    void saveFailed_UseBalance() {
        // given
        AccountUser user = AccountUser.builder().name("Pobi").build();
        user.setId(12L);

        Account account = Account.builder()
                .accountUser(user)
                .accountStatus(AccountStatus.IN_USE)
                .balance(10000L)
                .accountNumber("1000000012").build();

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));

        given(transactionRepository.save(any()))
                .willReturn(Transaction.builder()
                        .account(account)
                        .transactionType(USE)
                        .transactionResultType(S)
                        .amount(1000L)
                        .balanceSnapshot(9000L)
                        .transactionId("transactionId")
                        .transactedAt(LocalDateTime.now())
                        .build()
                );

        ArgumentCaptor<Transaction> captor =
                ArgumentCaptor.forClass(Transaction.class);

        // when
        transactionService.saveFailedUseTransaction(
                "10000000000",
                200L
        );

        // then
        verify(transactionRepository, times(1))
                .save(captor.capture());
        assertEquals(F, captor.getValue().getTransactionResultType());
        assertEquals(USE, captor.getValue().getTransactionType());
        assertEquals(10000L, captor.getValue().getBalanceSnapshot());
        assertEquals(200L, captor.getValue().getAmount());
    }

    @Test
    void successCancelBalance() {
        // given
        AccountUser user = AccountUser.builder().name("Pobi").build();
        user.setId(12L);

        Account account = Account.builder()
                .accountUser(user)
                .accountStatus(AccountStatus.IN_USE)
                .balance(10000L)
                .accountNumber("1000000012").build();

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));

        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(
                        Transaction.builder()
                                .account(account)
                                .transactionType(USE)
                                .transactionResultType(S)
                                .amount(1000L)
                                .balanceSnapshot(9000L)
                                .transactionId("transactionId")
                                .transactedAt(LocalDateTime.now())
                                .build()
                        )
                );

        given(transactionRepository.save(any()))
                .willReturn(Transaction.builder()
                        .account(account)
                        .transactionType(CANCEL)
                        .transactionResultType(S)
                        .amount(1000L)
                        .balanceSnapshot(9000L)
                        .transactionId("transactionIdForCancel")
                        .transactedAt(LocalDateTime.now())
                        .build()
                );

        ArgumentCaptor<Transaction> captor =
                ArgumentCaptor.forClass(Transaction.class);

        // when
        TransactionDto transactionDto = transactionService.cancelBalance(
                "transactionId", "10000000000", 1000L
        );

        // then
        verify(transactionRepository, times(1))
                .save(captor.capture());
        assertEquals(S, captor.getValue().getTransactionResultType());
        assertEquals(CANCEL, captor.getValue().getTransactionType());
        assertEquals(11000L, captor.getValue().getBalanceSnapshot());
        assertEquals(1000L, captor.getValue().getAmount());
        assertEquals(S, transactionDto.getTransactionResultType());
        assertEquals(CANCEL, transactionDto.getTransactionType());
        assertEquals(9000L, transactionDto.getBalanceSnapshot());
        assertEquals(1000L, transactionDto.getAmount());
    }

    @Test
    @DisplayName("해당 계좌 없음 - 잔액 사용 취소 실패")
    void cancelBalance_AccountNotFound() {
        // given
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(
                            Transaction.builder()
                                    .transactionType(CANCEL)
                                    .transactionResultType(S)
                                    .amount(1000L)
                                    .balanceSnapshot(9000L)
                                    .transactionId("transactionId")
                                    .transactedAt(LocalDateTime.now())
                                    .build()
                        )
                );

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.empty());

        // when
        AccountException accountException = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance(
                        "transactionId", "10000000000", 200L
                ));

        // then
        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, accountException.getErrorCode());
    }

    @Test
    @DisplayName("해당 거래 없음 - 잔액 사용 취소 실패")
    void cancelBalance_TransactionNotFound() {
        // given
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.empty());

        // when
        AccountException accountException = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance(
                        "transactionId", "10000000000", 200L
                ));

        // then
        assertEquals(ErrorCode.TRANSACTION_NOT_FOUND, accountException.getErrorCode());
    }

    @Test
    @DisplayName("거래와 계좌 매칭 실패 - 잔액 사용 취소 실패")
    void cancelBalance_TransactionAccountUnMatch() {
        // given
        AccountUser user = AccountUser.builder().name("Pobi").build();
        user.setId(12L);

        Account account = Account.builder()
                .accountUser(user)
                .accountStatus(AccountStatus.IN_USE)
                .balance(10000L)
                .accountNumber("1000000012").build();
        account.setId(1L);

        Account accountNotUse = Account.builder()
                .accountUser(user)
                .accountStatus(AccountStatus.IN_USE)
                .balance(10000L)
                .accountNumber("1000000013").build();
        accountNotUse.setId(2L);

        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(
                                Transaction.builder()
                                        .account(account)
                                        .transactionType(USE)
                                        .transactionResultType(S)
                                        .amount(1000L)
                                        .balanceSnapshot(9000L)
                                        .transactionId("transactionId")
                                        .transactedAt(LocalDateTime.now())
                                        .build()
                        )
                );

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(accountNotUse));

        // when
        AccountException accountException = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance(
                        "transactionId",
                        "10000000000",
                        1000L
                ));

        // then
        assertEquals(ErrorCode.TRANSACTION_ACCOUNT_UN_MATCH, accountException.getErrorCode());
    }

    @Test
    @DisplayName("거래금액과 취소금액이 다름 - 잔액 사용 취소 실패")
    void cancelBalance_UseAmountCancelAmountUnMatch() {
        // given
        AccountUser user = AccountUser.builder().name("Pobi").build();
        user.setId(12L);

        Account account = Account.builder()
                .accountUser(user)
                .accountStatus(AccountStatus.IN_USE)
                .balance(10000L)
                .accountNumber("1000000012").build();
        account.setId(1L);

        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(
                                Transaction.builder()
                                        .account(account)
                                        .transactionType(USE)
                                        .transactionResultType(S)
                                        .amount(2000L)
                                        .balanceSnapshot(9000L)
                                        .transactionId("transactionId")
                                        .transactedAt(LocalDateTime.now())
                                        .build()
                        )
                );

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));

        // when
        AccountException accountException = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance(
                        "transactionId",
                        "10000000000",
                        1000L
                ));

        // then
        assertEquals(ErrorCode.CANCEL_MUST_FULLY, accountException.getErrorCode());
    }

    @Test
    @DisplayName("거래 취소는 1년 까지만 가능 - 잔액 사용 취소 실패")
    void cancelBalance_TooOldOrderToCancel() {
        // given
        AccountUser user = AccountUser.builder().name("Pobi").build();
        user.setId(12L);

        Account account = Account.builder()
                .accountUser(user)
                .accountStatus(AccountStatus.IN_USE)
                .balance(10000L)
                .accountNumber("1000000012").build();
        account.setId(1L);

        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(
                                Transaction.builder()
                                        .account(account)
                                        .transactionType(USE)
                                        .transactionResultType(S)
                                        .amount(1000L)
                                        .balanceSnapshot(9000L)
                                        .transactionId("transactionId")
                                        .transactedAt(LocalDateTime.now()
                                                .minusYears(1))
//                                                .minusYears(1).minusDays(1))
                                        .build()
                        )
                );

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));

        // when
        AccountException accountException = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance(
                        "transactionId",
                        "10000000000",
                        1000L
                ));

        // then
        assertEquals(ErrorCode.TOO_OLD_ORDER_TO_CANCEL, accountException.getErrorCode());
    }

    @Test
    void successQueryTransaction() {
        // given
        AccountUser user = AccountUser.builder().name("Pobi").build();
        user.setId(12L);

        Account account = Account.builder()
                .accountUser(user)
                .accountStatus(AccountStatus.IN_USE)
                .balance(10000L)
                .accountNumber("1000000012").build();
        account.setId(1L);

        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(
                                Transaction.builder()
                                        .account(account)
                                        .transactionType(USE)
                                        .transactionResultType(S)
                                        .amount(2000L)
                                        .balanceSnapshot(9000L)
                                        .transactionId("transactionId")
                                        .transactedAt(LocalDateTime.now())
                                        .build()
                        )
                );

        // when
        TransactionDto transactionDto =
                transactionService.queryTranscation("transactionId");

        // then
        assertEquals("1000000012", transactionDto.getAccountNumber());
        assertEquals(USE, transactionDto.getTransactionType());
        assertEquals(S, transactionDto.getTransactionResultType());
        assertEquals("transactionId", transactionDto.getTransactionId());
        assertEquals(2000L, transactionDto.getAmount());
    }

    @Test
    @DisplayName("원거래 없음 - 거래 조회 실패")
    void queryTransaction_TransactionNotFound() {
        // given
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.empty());

        // when
        AccountException accountException = assertThrows(AccountException.class,
                () -> transactionService.queryTranscation("transactionId"));

        // then
        assertEquals(ErrorCode.TRANSACTION_NOT_FOUND, accountException.getErrorCode());
    }
}