package cn.twinkle.middleware.dbs.router.dynamic;

import cn.twinkle.middleware.dbs.router.DBContextHolder;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * @Author: zhencym
 * @DATE: 2023/4/23
 * 四、
 * 动态数据源获取，每当切换数据源，都要从这个里面进行获取
 * 可以点进去看到，这个抽象类里保存了targetDataSources目标数据源  + defaultTargetDataSource默认数据源
 * 所以我们需要把这些信息都存到里面，以方便切换数据源：在自动配置类DataSourceAutoConfig 里的 dataSource方法实现了这一点
 *
 * 需要继承AbstractRoutingDataSource，来实现切换数据源的方法
 * 返回的是线程本地变量保存的数据库路由结果
 */
public class DynamicDataSource extends AbstractRoutingDataSource {

  /**
   * 这里返回数据库路由结果：db+两位路由序号
   * @return
   */
  @Override
  protected Object determineCurrentLookupKey() {
    return "db" + DBContextHolder.getDBKey();
  }
}
