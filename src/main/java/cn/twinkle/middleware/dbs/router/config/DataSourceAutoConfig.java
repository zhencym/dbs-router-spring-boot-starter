package cn.twinkle.middleware.dbs.router.config;

import cn.twinkle.middleware.dbs.router.DBRouterConfig;
import cn.twinkle.middleware.dbs.router.DBRouterJoinPoint;
import cn.twinkle.middleware.dbs.router.dynamic.DynamicDataSource;
import cn.twinkle.middleware.dbs.router.dynamic.DynamicMybatisPlugin;
import cn.twinkle.middleware.dbs.router.strategy.IDBRouterStrategy;
import cn.twinkle.middleware.dbs.router.strategy.impl.DBRouterStrategyHashCode;
import cn.twinkle.middleware.dbs.router.util.PropertyUtil;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.apache.ibatis.plugin.Interceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @Author: zhencym
 * @DATE: 2023/4/23
 * 五、
 * 数据源配置解析
 * 即读取配置,需要实现EnvironmentAware里的setEnvironment方法
 * 配置类，注入所有对象、配置数据
 */
@Configuration
public class DataSourceAutoConfig implements EnvironmentAware {

  /**
   * 数据源配置组
   */
  private Map<String, Map<String, Object>> dataSourceMap = new HashMap<>();

  /**
   * 默认数据源配置
   */
  private Map<String, Object> defaultDataSourceConfig;

  /**
   * 分库数量
   */
  private int dbCount;

  /**
   * 分表数量
   */
  private int tbCount;

  /**
   * 路由字段
   */
  private String routerKey;

  /**
   * 切面类，实现AOP目标方法增强
   * @param dbRouterConfig
   * @param dbRouterStrategy
   * @return
   */
  @Bean(name = "db-router-point")
  @ConditionalOnMissingBean //不存在时注入
  public DBRouterJoinPoint point(DBRouterConfig dbRouterConfig, IDBRouterStrategy dbRouterStrategy) {
    return new DBRouterJoinPoint(dbRouterConfig, dbRouterStrategy);
  }

  /**
   * 读取yml文件中的dbCount、tbCount、routerKey参数
   * 并且赋给DBRouterConfig对象，以便 DynamicMybatisPlugin 在拦截切入时dao方法时，
   * 能够使用这些配置
   * @return
   */
  @Bean
  public DBRouterConfig dbRouterConfig() {
    return new DBRouterConfig(dbCount, tbCount, routerKey);
  }

  /**
   * 得到 Mybatis拦截器对象DynamicMybatisPlugin，
   * 用于拦截dao方法、更新表名，实现分表路由
   * @return
   */
  @Bean
  public Interceptor plugin() {
    return new DynamicMybatisPlugin();
  }

  /**
   * 配置所有数据源信息，并保存到DynamicDataSource对象
   * 里面的determineCurrentLookupKey方法用于切换数据源，实现分库路由
   * @return
   */
  @Bean
  public DataSource dataSource() {
    // 创建数据源
    Map<Object, Object> targetDataSources = new HashMap<>();
    // 根据所有其他数据源的名字、配置信息，建立每一个数据源的连接信息，并保存到targetDataSources
    for (String dbInfo : dataSourceMap.keySet()) {
      Map<String, Object> objMap = dataSourceMap.get(dbInfo);
      // 每一个分库，对应一个连接，比如db01--连接
      targetDataSources.put(dbInfo, new DriverManagerDataSource(objMap.get("url").toString(),
          objMap.get("username").toString(), objMap.get("password").toString()));
    }

    // 设置数据源
    // 把每个数据源连接信息，保存到DynamicDataSource，并交给IOC
    DynamicDataSource dynamicDataSource = new DynamicDataSource();
    dynamicDataSource.setTargetDataSources(targetDataSources);
    // 同时设置当前数据源为默认数据源
    dynamicDataSource.setDefaultTargetDataSource(new DriverManagerDataSource(defaultDataSourceConfig.get("url").toString(),
        defaultDataSourceConfig.get("username").toString(), defaultDataSourceConfig.get("password").toString()));

    return dynamicDataSource;
  }

  /**
   * 根据dbRouterConfig初始化IDBRouterStrategy
   * 用于数据源路由，并保存路由结果到ThreadLocal
   * 方便外部使用硬编码方式实现分库分表路由
   * @param dbRouterConfig
   * @return
   */
  @Bean
  public IDBRouterStrategy dbRouterStrategy(DBRouterConfig dbRouterConfig) {
    return new DBRouterStrategyHashCode(dbRouterConfig);
  }

  /**
   * 提供对事务管理 transactionTemplate
   * @param dataSource
   * @return
   */
  @Bean
  public TransactionTemplate transactionTemplate(DataSource dataSource) {
    DataSourceTransactionManager dataSourceTransactionManager = new DataSourceTransactionManager();
    dataSourceTransactionManager.setDataSource(dataSource);

    TransactionTemplate transactionTemplate = new TransactionTemplate();
    transactionTemplate.setTransactionManager(dataSourceTransactionManager);
    transactionTemplate.setPropagationBehaviorName("PROPAGATION_REQUIRED");
    return transactionTemplate;
  }

  /**
   * 读取yml数据，初始化dbCount、tbCount、routerKey
   * 初始化默认数据库名字、配置信息 defaultDataSourceConfig
   * 初始化其他数据库名字、配置信息 dataSourceMap
   * @param environment
   */
  @Override
  public void setEnvironment(Environment environment) {
    String prefix = "mini-db-router.jdbc.datasource.";

    dbCount = Integer.valueOf(environment.getProperty(prefix + "dbCount"));
    tbCount = Integer.valueOf(environment.getProperty(prefix + "tbCount"));
    routerKey = environment.getProperty(prefix + "routerKey");

    // 分库分表数据源
    // 其他数据库源list
    String dataSources = environment.getProperty(prefix + "list");
    assert dataSources != null;
    for (String dbInfo : dataSources.split(",")) {
      // 获取每个数据源名字，并且根据名字获取数据源的配置
      Map<String, Object> dataSourceProps = PropertyUtil.handle(environment, prefix + dbInfo, Map.class);
      // 最终数据源名字 + 数据源配置 保存在 dataSourceMap中
      dataSourceMap.put(dbInfo, dataSourceProps);
    }

    // 默认数据源，默认数据源名字 + 默认数据源配置 保存在defaultDataSourceConfig
    String defaultData = environment.getProperty(prefix + "default");
    defaultDataSourceConfig = PropertyUtil.handle(environment, prefix + defaultData, Map.class);

  }
}
