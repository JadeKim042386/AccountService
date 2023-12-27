package com.example.account.dto;

import com.example.account.type.TransactionResultType;
import com.example.account.type.TransactionType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class QueryTransactionResponse {
    private String accountNumber;
    private TransactionType transactionType;
    private TransactionResultType transactionResult;
    private String transactionId;
    private Long amount;
    private LocalDateTime transactedAt;
    public static QueryTransactionResponse from(TransactionDto dto) {
        return QueryTransactionResponse.builder()
                .accountNumber(dto.getAccountNumber())
                .transactionType(dto.getTransactionType())
                .transactionResult(dto.getTransactionResultType())
                .transactionId(dto.getTransactionId())
                .amount(dto.getAmount())
                .transactedAt(dto.getTransactedAt())
                .build();
    }
}
