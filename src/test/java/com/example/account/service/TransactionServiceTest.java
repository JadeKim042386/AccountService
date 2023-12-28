package com.example.account.service;

import com.example.account.domain.Account;
import com.example.account.domain.AccountUser;
import com.example.account.domain.Transaction;
import com.example.account.dto.TransactionDto;
import com.example.account.exception.AccountException;
import com.example.account.repository.AccountRepository;
import com.example.account.repository.AccountUserRepository;
import com.example.account.repository.TransactionRepository;
import com.example.account.type.AccountStatus;
import com.example.account.type.ErrorCode;
import com.example.account.type.TransactionResultType;
import com.example.account.type.TransactionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.example.account.type.AccountStatus.UNREGISTERED;
import static com.example.account.type.TransactionResultType.S;
import static com.example.account.type.TransactionType.USE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    void successUseBalance() throws InterruptedException {
        //given
        AccountUser user = AccountUser.builder().id(12L).name("joo").build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        Account account = Account.builder().accountUser(user).accountStatus(AccountStatus.IN_USE).accountNumber("1000000012").balance(10000L).build();
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));
        given(transactionRepository.save(any())).willReturn(Transaction.builder()
                        .account(account)
                        .transactionType(USE)
                        .transactionResultType(S)
                        .transactionId("transactionId")
                        .transactedAt(LocalDateTime.now())
                        .amount(1000L)
                        .balanceSnapshot(9000L)
                        .build()
                );
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        //when
        TransactionDto transactionDto = transactionService.useBalance(1L, "1000000000", 200L);
        //then
        verify(transactionRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getAmount()).isEqualTo(200L);
        assertThat(captor.getValue().getBalanceSnapshot()).isEqualTo(9800L);
        assertThat(transactionDto.getTransactionResultType()).isEqualTo(S);
        assertThat(transactionDto.getTransactionType()).isEqualTo(USE);
        assertThat(transactionDto.getBalanceSnapshot()).isEqualTo(9000L);
        assertThat(transactionDto.getAmount()).isEqualTo(1000L);
    }

    @Test
    @DisplayName("해당 유저 없음 - 잔액 사용 실패")
    void createAccount_UserNotFound() {
        //given
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.empty());
        //when
        assertThatThrownBy(() -> transactionService.useBalance(1L, "1234567890", 1000L))
                .isInstanceOf(AccountException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
        //then

    }

    @Test
    @DisplayName("해당 계좌 없음 - 잔액 사용 실패")
    void deleteAccount_AccountNotFound() {
        //given
        AccountUser user = AccountUser.builder().id(12L).name("joo").build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.empty());
        //when
        assertThatThrownBy(() -> transactionService.useBalance(1L, "1234567890", 1000L))
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
        assertThatThrownBy(() -> transactionService.useBalance(1L, "1234567890", 1000L))
                .isInstanceOf(AccountException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_ACCOUNT_UNMATCH);
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
        assertThatThrownBy(() -> transactionService.useBalance(1L, "1234567890", 1000L))
                .isInstanceOf(AccountException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCOUNT_ALREADY_UNREGISTERED);
        //then
    }

    @Test
    @DisplayName("거래 금액이 잔액보다 큰 경우")
    void exceedAmount_UseBalance() {
        //given
        AccountUser user = AccountUser.builder().id(12L).name("joo").build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        Account account = Account.builder().accountUser(user).accountStatus(AccountStatus.IN_USE).accountNumber("1000000012").balance(100L).build();
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));

        //when
        assertThatThrownBy(() -> transactionService.useBalance(1L, "1234567890", 1000L))
                .isInstanceOf(AccountException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AMOUNT_EXCEED_BALANCE);
        //then
        verify(transactionRepository, times(0)).save(any());
    }

    @Test
    @DisplayName("실패 트랜잭션 저장")
    void savedFailedUseTransaction() {
        //given
        AccountUser user = AccountUser.builder().id(12L).name("joo").build();
        Account account = Account.builder().accountUser(user).accountStatus(AccountStatus.IN_USE).accountNumber("1000000012").balance(10000L).build();
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));
        given(transactionRepository.save(any())).willReturn(Transaction.builder()
                .account(account)
                .transactionType(USE)
                .transactionResultType(TransactionResultType.F)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now())
                .amount(1000L)
                .balanceSnapshot(9000L)
                .build()
        );
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        //when
        transactionService.saveFailedUseTransaction("1000000000", 200L);
        //then
        verify(transactionRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getAmount()).isEqualTo(200L);
        assertThat(captor.getValue().getBalanceSnapshot()).isEqualTo(10000L);
        assertThat(captor.getValue().getTransactionResultType()).isEqualTo(TransactionResultType.F);
    }

    @Test
    void successCancelBalance() {
        //given
        AccountUser user = AccountUser.builder().id(12L).name("joo").build();
        Account account = Account.builder()
                .accountUser(user)
                .accountStatus(AccountStatus.IN_USE)
                .accountNumber("1000000012")
                .balance(10000L)
                .build();
        Transaction transaction = Transaction.builder()
                .account(account)
                .transactionType(USE)
                .transactionResultType(S)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now())
                .amount(200L)
                .balanceSnapshot(9000L)
                .build();
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));
        given(transactionRepository.save(any())).willReturn(Transaction.builder()
                .account(account)
                .transactionType(TransactionType.CANCEL)
                .transactionResultType(S)
                .transactionId("transactionIdForCancel")
                .transactedAt(LocalDateTime.now())
                .amount(200L)
                .balanceSnapshot(10000L)
                .build()
        );
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        //when
        TransactionDto transactionDto = transactionService.cancelBalance(
                "transactionId",
                "1000000000",
                200L
        );
        //then
        verify(transactionRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getAmount()).isEqualTo(200L);
        assertThat(captor.getValue().getBalanceSnapshot()).isEqualTo(10000L + 200L);
        assertThat(transactionDto.getTransactionResultType()).isEqualTo(S);
        assertThat(transactionDto.getTransactionType()).isEqualTo(TransactionType.CANCEL);
        assertThat(transactionDto.getBalanceSnapshot()).isEqualTo(10000L);
        assertThat(transactionDto.getAmount()).isEqualTo(200L);
    }
    @Test
    @DisplayName("해당 계좌 없음 - 잔액 사용 취소 실패")
    void cancelTransaction_AccountNotFound() {
        //given
        Transaction transaction = Transaction.builder().build();
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.empty());
        //when
        assertThatThrownBy(() -> transactionService.cancelBalance("transactionId", "1234567890", 1000L))
                .isInstanceOf(AccountException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCOUNT_NOT_FOUND);
        //then

    }

    @Test
    @DisplayName("해당 거래 없음 - 잔액 사용 취소 실패")
    void cancelTransaction_TransactionNotFound() {
        //given
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.empty());
        //when
        assertThatThrownBy(() -> transactionService.cancelBalance("transactionId", "1234567890", 1000L))
                .isInstanceOf(AccountException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TRANSACTION_NOT_FOUND);
        //then

    }

    @Test
    @DisplayName("거래와 계좌의 매칭 실패 - 잔액 사용 취소 실패")
    void cancelTransaction_TransactionAccountUnMatch() {
        //given
        AccountUser user = AccountUser.builder().id(12L).name("joo").build();
        Account account = Account.builder()
                .id(1L)
                .accountUser(user)
                .accountStatus(AccountStatus.IN_USE)
                .accountNumber("1000000012")
                .balance(10000L)
                .build();
        Account accountNotUse = Account.builder()
                .id(2L)
                .accountUser(user)
                .accountStatus(AccountStatus.IN_USE)
                .accountNumber("1000000013")
                .balance(10000L)
                .build();
        Transaction transaction = Transaction.builder()
                .account(account)
                .transactionType(USE)
                .transactionResultType(S)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now())
                .amount(200L)
                .balanceSnapshot(9000L)
                .build();
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(accountNotUse));
        //when
        assertThatThrownBy(() -> transactionService.cancelBalance("transactionId", "1234567890", 200L))
                .isInstanceOf(AccountException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TRANSACTION_ACCOUNT_UNMATCH);
        //then

    }

    @Test
    @DisplayName("거래금액과 취소금액이 상이 - 잔액 사용 취소 실패")
    void cancelTransaction_CanceelMustFully() {
        //given
        AccountUser user = AccountUser.builder().id(12L).name("joo").build();
        Account account = Account.builder()
                .id(1L)
                .accountUser(user)
                .accountStatus(AccountStatus.IN_USE)
                .accountNumber("1000000012")
                .balance(10000L)
                .build();
        Transaction transaction = Transaction.builder()
                .account(account)
                .transactionType(USE)
                .transactionResultType(S)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now())
                .amount(200L + 1000L)
                .balanceSnapshot(9000L)
                .build();
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));
        //when
        assertThatThrownBy(() -> transactionService.cancelBalance("transactionId", "1234567890", 200L))
                .isInstanceOf(AccountException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CANCEL_MUST_FULLY);
        //then

    }

    @Test
    @DisplayName("취소는 1년까지만 가능 - 잔액 사용 취소 실패")
    void cancelTransaction_TooOldOrder() {
        //given
        AccountUser user = AccountUser.builder().id(12L).name("joo").build();
        Account account = Account.builder()
                .id(1L)
                .accountUser(user)
                .accountStatus(AccountStatus.IN_USE)
                .accountNumber("1000000012")
                .balance(10000L)
                .build();
        Transaction transaction = Transaction.builder()
                .account(account)
                .transactionType(USE)
                .transactionResultType(S)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now().minusYears(1).minusDays(1))
                .amount(200L)
                .balanceSnapshot(9000L)
                .build();
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));
        //when
        assertThatThrownBy(() -> transactionService.cancelBalance("transactionId", "1234567890", 200L))
                .isInstanceOf(AccountException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TOO_OLD_ORDER_TO_CANCEL);
        //then

    }

    @Test
    void successQueryTransaction() {
        //given
        AccountUser user = AccountUser.builder().id(12L).name("joo").build();
        Account account = Account.builder()
                .id(1L)
                .accountUser(user)
                .accountStatus(AccountStatus.IN_USE)
                .accountNumber("1000000012")
                .balance(10000L)
                .build();
        Transaction transaction = Transaction.builder()
                .account(account)
                .transactionType(USE)
                .transactionResultType(S)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now().minusYears(1).minusDays(1))
                .amount(200L)
                .balanceSnapshot(9000L)
                .build();
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));
        //when
        TransactionDto transactionDto = transactionService.queryTransaction("transactionId");
        //then
        assertThat(transactionDto.getTransactionType()).isEqualTo(USE);
        assertThat(transactionDto.getTransactionResultType()).isEqualTo(S);
        assertThat(transactionDto.getAmount()).isEqualTo(200L);
        assertThat(transactionDto.getTransactionId()).isEqualTo("transactionId");
    }

    @Test
    @DisplayName("해당 거래 없음 - 잔액 사용 취소 실패")
    void queryTransaction_TransactionNotFound() {
        //given
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.empty());
        //when
        assertThatThrownBy(() -> transactionService.queryTransaction("transactionId"))
                .isInstanceOf(AccountException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TRANSACTION_NOT_FOUND);
        //then

    }
}