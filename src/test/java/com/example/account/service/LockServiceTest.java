package com.example.account.service;

import com.example.account.exception.AccountException;
import com.example.account.type.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class LockServiceTest {
    @InjectMocks
    private LockService lockService;
    @Mock
    private RedissonClient redissonClient;
    @Mock
    private RLock rLock;

    @Test
    void successGetLock() throws InterruptedException {
        //given
        given(redissonClient.getLock(anyString())).willReturn(rLock);
        given(rLock.tryLock(anyLong(), anyLong(), any())).willReturn(true);
        //when
        assertDoesNotThrow(() -> lockService.lock("123"));
        //then
    }

    @Test
    void failGetLock() throws InterruptedException {
        //given
        given(redissonClient.getLock(anyString())).willReturn(rLock);
        given(rLock.tryLock(anyLong(), anyLong(), any())).willReturn(false);
        //when
        assertThatThrownBy(() -> lockService.lock("123"))
                .isInstanceOf(AccountException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCOUNT_TRANSACTION_LOCK);
        //then
    }
}