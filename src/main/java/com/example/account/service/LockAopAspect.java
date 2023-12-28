package com.example.account.service;

import com.example.account.aop.AccountLockIdInterface;
import com.example.account.repository.RedisLockRepository;
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
    private final RedisLockRepository redisLockRepository;

    @Around("@annotation(com.example.account.aop.AccountLock) && args(request)")
    public Object aroundMethod(
            ProceedingJoinPoint pjp,
            AccountLockIdInterface request
    ) throws Throwable {
        //lock
        while (!redisLockRepository.lock(request.getAccountNumber())) {
            log.warn("해당 계좌는 사용 중입니다.");
            Thread.sleep(100); //0.1초
        }
        try {
            return pjp.proceed();
        } finally {
            //unlock
            redisLockRepository.unlock(request.getAccountNumber());
        }
    }
}
