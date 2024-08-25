package com.account.service;

import com.account.aop.AccountLockIdInterface;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class LockAopAspect {
    private final RedisLockService redisLockService;

    // before after 에 해야하는 동작을 따로 표시해주지 않아도 around 한 개로 가능
    @Around("@annotation(com.account.aop.AccountLock) && args(request)")
    public Object aroundMethod(
            ProceedingJoinPoint pjp,
            AccountLockIdInterface request
    ) throws Throwable {
        // lock 취득 시도
        redisLockService.accountLock(request.getAccountNumber());
        try {
            // aop 를 걸어줬던 그 부분을 동작 시킨다.
            return pjp.proceed();
        } finally {
            // 동작이 성공하던 실패하던 무조건 lock 해제
            redisLockService.accountUnLock(request.getAccountNumber());
        }
    }
}
