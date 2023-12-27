package com.example.account.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AccountInfo {
    private String accountNumber;
    private Long balance;

    public static AccountInfo fromDto(AccountDto accountDto) {
        return AccountInfo.builder()
                .accountNumber(accountDto.getAccountNumber())
                .balance(accountDto.getBalance())
                .build();
    }
}
