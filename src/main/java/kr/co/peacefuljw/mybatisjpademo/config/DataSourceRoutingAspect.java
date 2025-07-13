package kr.co.peacefuljw.mybatisjpademo.config;

import kr.co.peacefuljw.mybatisjpademo.common.annotation.ReadOnly;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Slf4j
@Aspect
@Component
@Order(0) // 트랜잭션보다 먼저 실행
public class DataSourceRoutingAspect {

    @Before("@annotation(kr.co.peacefuljw.mybatisjpademo.common.annotation.ReadOnly)")
    public void setReadOnlyDataSource(JoinPoint joinPoint) {
        log.debug("ReadOnly 메서드 호출: {}", joinPoint.getSignature().getName());
        DataSourceContextHolder.setDataSourceType(DataSourceType.SLAVE);
    }

    @Before("@annotation(org.springframework.transaction.annotation.Transactional)")
    public void setTransactionalDataSource(JoinPoint joinPoint) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();

            // ReadOnly 어노테이션이 있는지 확인
            ReadOnly readOnly = method.getAnnotation(ReadOnly.class);
            if (readOnly == null) {
                // 클래스 레벨에서도 확인
                readOnly = method.getDeclaringClass().getAnnotation(ReadOnly.class);
            }

            if (readOnly == null) {
                log.debug("Transactional 메서드 호출 (쓰기): {}", joinPoint.getSignature().getName());
                DataSourceContextHolder.setDataSourceType(DataSourceType.MASTER);
            }
        } catch (Exception e) {
            log.warn("DataSource 라우팅 설정 중 오류: {}", e.getMessage());
            DataSourceContextHolder.setDataSourceType(DataSourceType.MASTER);
        }
    }

    @After("@annotation(kr.co.peacefuljw.mybatisjpademo.common.annotation.ReadOnly) || " +
            "@annotation(org.springframework.transaction.annotation.Transactional)")
    public void clearDataSource(JoinPoint joinPoint) {
        log.debug("메서드 완료 후 DataSource 컨텍스트 정리: {}", joinPoint.getSignature().getName());
        DataSourceContextHolder.clear();
    }
}
