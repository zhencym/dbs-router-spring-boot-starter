package cn.twinkle.middleware.dbs.router;

/**
 * @Author: zhencym
 * @DATE: 2023/4/23
 *
 * 数据源上下文
 * 即用于保存路由的结果；路由到那个库，那个表
 * 使用ThreadLocal，保存根据键值分库分表的路由结果
 * 保证每个线程都有自己的一份路由结果的数据，防止造成竞争，避免同步
 */
public class DBContextHolder {
  private static final ThreadLocal<String> dbKey = new ThreadLocal<>();
  private static final ThreadLocal<String> tbKey = new ThreadLocal<>();

  public static void setDBKey(String dbKeyIdx){
    dbKey.set(dbKeyIdx);
  }

  public static String getDBKey(){
    return dbKey.get();
  }

  public static void setTBKey(String tbKeyIdx){
      tbKey.set(tbKeyIdx);
  }

  public static String getTBKey(){
    return tbKey.get();
  }

  public static void clearDBKey(){
    dbKey.remove();
  }

  public static void clearTBKey(){
    tbKey.remove();
  }
}
