package com.account.service;

import com.account.exception.AccountException;
import com.account.type.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class RedisLockServiceTest {
    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock rLock;

    @InjectMocks
    private RedisLockService redisLockService;

    @Test
    void successGetLock() throws InterruptedException {
        // given
        given(redissonClient.getLock(anyString()))
                .willReturn(rLock);
        given(rLock.tryLock(anyLong(), anyLong(), any()))
                .willReturn(true);

        // when
        // then
        assertDoesNotThrow(
                () -> redisLockService.accountUnLock("123")
        );
    }

    @Test
    void failGetLock() throws InterruptedException {
        // given
        given(redissonClient.getLock(anyString()))
                .willReturn(rLock);
        given(rLock.tryLock(anyLong(), anyLong(), any()))
                .willReturn(false);

        // when
        AccountException exception = assertThrows(
                AccountException.class,
                () -> redisLockService.accountUnLock("123")
        );

        // then
        assertEquals(
                ErrorCode.ACCOUNT_TRANSACTION_LOCK,
                exception.getErrorCode()
        );
        assertEquals(
                ErrorCode.ACCOUNT_TRANSACTION_LOCK.getDescription(),
                exception.getErrorMessage()
        );
    }
}