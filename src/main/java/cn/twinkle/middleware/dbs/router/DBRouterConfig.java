package cn.twinkle.middleware.dbs.router;

/**
 * @Author: zhencym
 * @DATE: 2023/4/23
 * 数据路由配置
 * 用于保存读取yml文件中的分库数量、分表数量、默认路由字段属性
 */
public class DBRouterConfig {
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

  public DBRouterConfig() {
  }

  public DBRouterConfig(int dbCount, int tbCount, String routerKey) {
    this.dbCount = dbCount;
    this.tbCount = tbCount;
    this.routerKey = routerKey;
  }

  public int getDbCount() {
    return dbCount;
  }

  public void setDbCount(int dbCount) {
    this.dbCount = dbCount;
  }

  public int getTbCount() {
    return tbCount;
  }

  public void setTbCount(int tbCount) {
    this.tbCount = tbCount;
  }

  public String getRouterKey() {
    return routerKey;
  }

  public void setRouterKey(String routerKey) {
    this.routerKey = routerKey;
  }
}
