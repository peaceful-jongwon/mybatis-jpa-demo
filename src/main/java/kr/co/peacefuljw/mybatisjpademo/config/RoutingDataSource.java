package kr.co.peacefuljw.mybatisjpademo.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;

@Slf4j
public class RoutingDataSource extends AbstractRoutingDataSource {

    @Override
    protected Object determineCurrentLookupKey() {
        DataSourceType dataSourceType = DataSourceContextHolder.getDataSourceType();
        log.debug("라우팅 데이터소스 결정: {}", dataSourceType);
        return dataSourceType != null ? dataSourceType : DataSourceType.MASTER;
    }

    @Override
    protected DataSource determineTargetDataSource() {
        DataSource dataSource = super.determineTargetDataSource();
        log.debug("선택된 데이터소스: {}", dataSource.getClass().getSimpleName());
        return dataSource;
    }
}
