package com.account.service;

import com.account.dto.UseBalance;
import com.account.exception.AccountException;
import com.account.type.ErrorCode;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LockAopAspectTest {
    @Mock
    RedisLockService redisLockService;

    @Mock
    private ProceedingJoinPoint proceedingJoinPoint;

    @InjectMocks
    private LockAopAspect lockAopAspect;

    @Test
    void lockAndUnLock() throws Throwable {
        // given
        ArgumentCaptor<String> lockArgumentCaptor =
                ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> unLockArgumentCaptor =
                ArgumentCaptor.forClass(String.class);

        UseBalance.Request request = new UseBalance.Request(
                123L,
                "1234567890",
                1000L
        );

        given(proceedingJoinPoint.proceed())
                .willThrow(new AccountException(ErrorCode.ACCOUNT_NOT_FOUND));

        // when
        assertThrows(
                AccountException.class,
                () -> lockAopAspect.aroundMethod(proceedingJoinPoint, request)
        );

        // then
        verify(redisLockService, times(1))
                .accountLock(lockArgumentCaptor.capture());
        verify(redisLockService, times(1))
                .accountUnLock(unLockArgumentCaptor.capture());

        assertEquals("1234567890", lockArgumentCaptor.getValue());
        assertEquals("1234567890", unLockArgumentCaptor.getValue());

    }
}