package com.account.dto;

import com.account.domain.Transaction;
import com.account.type.TransactionResultType;
import com.account.type.TransactionType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionDto {
    // controller 와 service 간의 데이터를 주고받기위한 DTO
    private String accountNumber;
    private TransactionType transactionType;
    private TransactionResultType transactionResultType;
    private Long amount;
    private Long balanceSnapshot;
    private String transactionId;
    private LocalDateTime transactedAt;

    // Dto 는 Entity 를 통해서 만들어지는 경우가 가장 많아서
    // 이렇게 static 메소드로 Entity 에서 Dto 로 변해준다.
    // 가독성도 좋고 안전하게 생성할수 있다.
    public static TransactionDto fromEntity(Transaction transaction) {
        return TransactionDto.builder()
                .accountNumber(transaction.getAccount().getAccountNumber())
                .transactionType(transaction.getTransactionType())
                .transactionResultType(transaction.getTransactionResultType())
                .amount(transaction.getAmount())
                .balanceSnapshot(transaction.getBalanceSnapshot())
                .transactionId(transaction.getTransactionId())
                .transactedAt(transaction.getTransactedAt())
                .build();
    }
}
