package cn.twinkle.middleware.dbs.router.strategy;

/**
 * @Author: zhencym
 * @DATE: 2023/4/23
 * 路由策略接口
 */
public interface IDBRouterStrategy {
  /**
   * 根据路由字段进行路由计算，得到路由结果
   * @param dbKeyAttr 路由字段
   */
  void doRouter(String dbKeyAttr);

  /**
   * 手动设置分表路由
   * @param dbIdx 路由库，需要在配置范围内
   */
  void setDBKey(int dbIdx);

  /**
   * 手动设置分表路由
   * @param tbIdx 路由表，需要在配置范围内
   */
  void setTBKey(int tbIdx);

  /**
   * 返回分表路由
   */
  String getDBKey();

  /**
   * 返回分表路由
   */
  String getTBKey();

  /**
   * 获取分库数量
   * @return 数量
   */
  int dbCount();

  /**
   * 获取分表数量
   * @return 数量
   */
  int tbCount();

  /**
   * 清除路由
   */
  void clear();
}
