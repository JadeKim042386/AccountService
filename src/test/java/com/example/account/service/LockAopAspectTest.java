package com.example.account.service;

import com.example.account.dto.UseBalance;
import com.example.account.exception.AccountException;
import com.example.account.type.ErrorCode;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LockAopAspectTest {
    @InjectMocks
    LockAopAspect lockAopAspect;

    @Mock
    private LockService lockService;
    @Mock
    private ProceedingJoinPoint proceedingJoinPoint;

    @Test
    void lockAndUnlock() throws Throwable {
        //given
        ArgumentCaptor<String> lockCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> unLockCaptor = ArgumentCaptor.forClass(String.class);
        UseBalance.Request request = new UseBalance.Request(123L, "1234", 1000L);
        willDoNothing().given(lockService).lock(anyString());
        //when
        lockAopAspect.aroundMethod(proceedingJoinPoint, request);
        //then
        verify(lockService, times(1)).lock(lockCaptor.capture());
        verify(lockService, times(1)).unlock(unLockCaptor.capture());
        assertThat(lockCaptor.getValue()).isEqualTo("1234");
        assertThat(unLockCaptor.getValue()).isEqualTo("1234");
    }

    @Test
    void lockAndUnlock_eventIfThrow() throws Throwable {
        //given
        ArgumentCaptor<String> lockCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> unLockCaptor = ArgumentCaptor.forClass(String.class);
        UseBalance.Request request = new UseBalance.Request(123L, "54321", 1000L);
        given(proceedingJoinPoint.proceed()).willThrow(new AccountException(ErrorCode.ACCOUNT_NOT_FOUND));
        //when
        assertThatThrownBy(() -> lockAopAspect.aroundMethod(proceedingJoinPoint, request))
                .isInstanceOf(AccountException.class);
        //then
        verify(lockService, times(1)).lock(lockCaptor.capture());
        verify(lockService, times(1)).unlock(unLockCaptor.capture());
        assertThat(lockCaptor.getValue()).isEqualTo("54321");
        assertThat(unLockCaptor.getValue()).isEqualTo("54321");
    }
}