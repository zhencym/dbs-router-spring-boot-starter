package cn.twinkle.middleware.dbs.router.strategy.impl;

import cn.twinkle.middleware.dbs.router.DBContextHolder;
import cn.twinkle.middleware.dbs.router.DBRouterConfig;
import cn.twinkle.middleware.dbs.router.strategy.IDBRouterStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Author: zhencym
 * @DATE: 2023/4/23
 * 路由策略提（分片策略）供两种方法：
 * 1、提供哈希路由策略，主要方法是doRouter
 * 2、提供手动路由策略，主要方法是setDBKey、setTBKey
 */
public class DBRouterStrategyHashCode implements IDBRouterStrategy {

  private Logger logger = LoggerFactory.getLogger(DBRouterStrategyHashCode.class);

  private DBRouterConfig dbRouterConfig;

  public DBRouterStrategyHashCode(DBRouterConfig dbRouterConfig) {
    this.dbRouterConfig = dbRouterConfig;
  }

  /**
   * 分库分表路由算法单独出来，方便调用
   * @param dbKeyAttr 路由字段
   */
  @Override
  public void doRouter(String dbKeyAttr) {
    // size = 分库数 * 分表数  ；并且分库分表数都是2的次幂，方便位运算
    int size = dbRouterConfig.getDbCount() * dbRouterConfig.getTbCount();

    // 扰动函数；在JDK 的 HashMap 中，对于一个元素的存放，需要进行哈希散列。而为了让散列更加均匀，所以添加了扰动函数。
    // 与上(size-1)，主要是因为a&(b-1)等于a%b,不过仅限于b等于2的n次幂 ； >>>无符号右移
    // 优点是计算快速，散列均匀，缺点是库表数仅限于2的n次幂
    // 所谓扰动函数，就是在获取哈希值之前先对hashCode的高位和低位进行异或操作，增大随机性，让分布更均匀
    int idx = (size-1) & (dbKeyAttr.hashCode() ^ (dbKeyAttr.hashCode() >>> 16));

    // 注意这里是 相除，商就是第几个分库
    int dbIdx = idx / dbRouterConfig.getTbCount() + 1;
    // 总数 - 第几个分库 * 每个分库的表数，就是当前分库的第几个表
    int tbIdx = idx - dbRouterConfig.getTbCount() * (dbIdx - 1);

    // 设置到 ThreadLocal
    DBContextHolder.setDBKey(String.format("%02d", dbIdx));
    DBContextHolder.setTBKey(String.format("%03d", tbIdx));
    logger.debug("数据库路由 dbIdx：{} tbIdx：{}",  dbIdx, tbIdx);
  }

  @Override
  public void setDBKey(int dbIdx) {
//    DBContextHolder.setDBKey(String.format("%02d", dbIdx));
    // 参数检验
    if (dbIdx > 0 && dbIdx <= this.dbCount()) {
      DBContextHolder.setDBKey(String.format("%02d", dbIdx));
    } else { //默认数据库1
      DBContextHolder.setDBKey(String.format("%02d", 1));
    }
  }

  @Override
  public void setTBKey(int tbIdx) {
//    DBContextHolder.setTBKey(String.format("%03d", tbIdx));
    // 参数检验
    if (tbIdx >= 0 && tbIdx < this.tbCount()) {
      DBContextHolder.setTBKey(String.format("%03d", tbIdx));
    } else { //默认表0
      DBContextHolder.setDBKey(String.format("%03d", 0));
    }
  }

  @Override
  public String getDBKey() {
    return DBContextHolder.getDBKey();
  }

  @Override
  public String getTBKey() {
    return DBContextHolder.getTBKey();
  }

  @Override
  public int dbCount() {
    return dbRouterConfig.getDbCount();
  }

  @Override
  public int tbCount() {
    return dbRouterConfig.getTbCount();
  }

  @Override
  public void clear() {
    DBContextHolder.clearDBKey();
    DBContextHolder.clearTBKey();
  }
}
