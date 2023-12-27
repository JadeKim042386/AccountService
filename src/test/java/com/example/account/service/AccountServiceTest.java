package com.example.account.service;

import com.example.account.domain.Account;
import com.example.account.domain.AccountUser;
import com.example.account.dto.AccountDto;
import com.example.account.exception.AccountException;
import com.example.account.repository.AccountUserRepository;
import com.example.account.type.AccountStatus;
import com.example.account.repository.AccountRepository;
import com.example.account.type.ErrorCode;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static com.example.account.type.AccountStatus.UNREGISTERED;
import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private AccountUserRepository accountUserRepository;

    @InjectMocks
    private AccountService accountService;

    @Test
    void createAccountSuccess() {
        //given
        AccountUser user = AccountUser.builder().id(12L).name("joo").build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        given(accountRepository.findFirstByOrderByIdDesc())
                .willReturn(Optional.of(Account.builder().accountNumber("1000000012").build()));
        given(accountRepository.save(any(Account.class)))
                .willReturn(Account.builder().accountUser(user).accountNumber("1000000013").build());
        //when
        AccountDto accountDto = accountService.createAccount(1L, 1000L);
        //then
        then(accountRepository).should().save(any(Account.class));
        assertThat(accountDto.getUserId()).isEqualTo(12L);
        assertThat(accountDto.getAccountNumber()).isEqualTo("1000000013");
    }

    @Test
    void createFirstAccountSuccess() {
        //given
        AccountUser user = AccountUser.builder().id(15L).name("joo").build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        given(accountRepository.findFirstByOrderByIdDesc())
                .willReturn(Optional.empty());
        given(accountRepository.save(any(Account.class)))
                .willReturn(Account.builder().accountUser(user).accountNumber("1000000015").build());
        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        //when
        AccountDto accountDto = accountService.createAccount(1L, 1000L);
        //then
        verify(accountRepository, times(1)).save(captor.capture());
        assertThat(accountDto.getUserId()).isEqualTo(15L);
        assertThat(captor.getValue().getAccountNumber()).isEqualTo("1000000000");
    }

    @Test
    @DisplayName("해당 유저 없음 - 계좌 생성 실패")
    void createAccount_UserNotFound() {
        //given
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.empty());
        //when
        assertThatThrownBy(() -> accountService.createAccount(1L, 1000L))
                .isInstanceOf(AccountException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
        //then

    }

    @Test
    void createAccount_maxAccountIs10() {
        //given
        AccountUser user = AccountUser.builder().id(12L).name("joo").build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        given(accountRepository.countByAccountUser(any(AccountUser.class)))
                .willReturn(10);
        //when
        assertThatThrownBy(() -> accountService.createAccount(1L, 1000L))
                .isInstanceOf(AccountException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MAX_ACCOUNT_PER_USER_10);
        //then
    }

    @Test
    void deleteAccountSuccess() {
        //given
        AccountUser user = AccountUser.builder().id(12L).name("joo").build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder().accountUser(user).balance(0L).accountNumber("1000000012").build()));
        //when
        AccountDto accountDto = accountService.deleteAccount(1L, "1000000012");
        //then
        assertThat(accountDto.getUserId()).isEqualTo(12L);
        assertThat(accountDto.getAccountNumber()).isEqualTo("1000000012");
    }
    @Test
    @DisplayName("해당 유저 없음 - 계좌 해지 실패")
    void deleteAccount_UserNotFound() {
        //given
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.empty());
        //when
        assertThatThrownBy(() -> accountService.deleteAccount(1L, "1000000012"))
                .isInstanceOf(AccountException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
        //then

    }

    @Test
    @DisplayName("해당 계좌 없음 - 계좌 해지 실패")
    void deleteAccount_AccountNotFound() {
        //given
        AccountUser user = AccountUser.builder().id(12L).name("joo").build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.empty());
        //when
        assertThatThrownBy(() -> accountService.deleteAccount(1L, "1000000012"))
                .isInstanceOf(AccountException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCOUNT_NOT_FOUND);
        //then

    }

    @Test
    @DisplayName("계좌 소유주 상이")
    void deleteAccountFailed_userUnMatch() {
        //given
        AccountUser user1 = AccountUser.builder().id(12L).name("joo").build();
        AccountUser user2 = AccountUser.builder().id(13L).name("ju").build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user1));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder().accountUser(user2).balance(0L).accountNumber("1000000012").build()));
        //when
        assertThatThrownBy(() -> accountService.deleteAccount(1L, "1000000012"))
                .isInstanceOf(AccountException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_ACCOUNT_UNMATCH);
        //then
    }

    @Test
    @DisplayName("잔액이 남아있을 경우")
    void deleteAccountFailed_notEmpty() {
        //given
        AccountUser user = AccountUser.builder().id(12L).name("joo").build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder().accountUser(user).balance(100L).accountNumber("1000000012").build()));
        //when
        assertThatThrownBy(() -> accountService.deleteAccount(1L, "1000000012"))
                .isInstanceOf(AccountException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BALANCE_NOT_EMPTY);
        //then
    }

    @Test
    @DisplayName("이미 해지된 계좌일 경우")
    void deleteAccountFailed_alreadyUnregistered() {
        //given
        AccountUser user = AccountUser.builder().id(12L).name("joo").build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder().accountUser(user).accountStatus(UNREGISTERED).balance(0L).accountNumber("1000000012").build()));
        //when
        assertThatThrownBy(() -> accountService.deleteAccount(1L, "1000000012"))
                .isInstanceOf(AccountException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCOUNT_ALREADY_UNREGISTERED);
        //then
    }

    @Test
    void successGetAccountsByUserId() {
        //given
        AccountUser user = AccountUser.builder().id(12L).name("joo").build();
        List<Account> accounts = List.of(
                Account.builder().accountUser(user).accountNumber("1234567890").balance(1000L).build(),
                Account.builder().accountUser(user).accountNumber("1111111111").balance(2000L).build(),
                Account.builder().accountUser(user).accountNumber("2222222222").balance(3000L).build()
        );
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        given(accountRepository.findByAccountUser(any()))
                .willReturn(accounts);
        //when
        List<AccountDto> accountDtos = accountService.getAccountsByUserId(1L);
        //then
        assertThat(accountDtos.size()).isEqualTo(3);
        assertThat(accountDtos.get(0).getAccountNumber()).isEqualTo("1234567890");
        assertThat(accountDtos.get(0).getBalance()).isEqualTo(1000);
        assertThat(accountDtos.get(1).getAccountNumber()).isEqualTo("1111111111");
        assertThat(accountDtos.get(1).getBalance()).isEqualTo(2000);
        assertThat(accountDtos.get(2).getAccountNumber()).isEqualTo("2222222222");
        assertThat(accountDtos.get(2).getBalance()).isEqualTo(3000);
    }

    @Test
    void failedGetAccounts() {
        //given
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.empty());
        //when
        assertThatThrownBy(() -> accountService.getAccountsByUserId(1L))
                .isInstanceOf(AccountException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
        //then
    }
}