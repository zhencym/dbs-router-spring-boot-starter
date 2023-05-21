package cn.twinkle.middleware.dbs.router.dynamic;

import cn.twinkle.middleware.dbs.router.DBContextHolder;
import cn.twinkle.middleware.dbs.router.annotation.DBRouterStrategy;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.DefaultReflectorFactory;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;

/**
 * @Author: zhencym
 * @DATE: 2023/4/23
 * Mybatis 拦截器，通过对 SQL 语句的拦截处理，修改分表信息
 *
 * @Intercepts是Mybatis中的拦截器，用于拦截sql执行
 * 首先mybatis拦截器可以拦截如下4中类型
 * Executor sql的内部执行器
 * ParameterHandler 拦截参数的处理
 * StatementHandler 拦截sql的构建
 * ResultSetHandler 拦截结果的处理
 *
 * @Signature 注解参数说明:
 * type：就是指定拦截器类型（ParameterHandler ，StatementHandler，ResultSetHandler ）
 * method：是拦截器类型中的方法，不是自己写的方法
 * args：是method中方法的入参
 */
@Intercepts({@Signature(type = StatementHandler.class,
    method = "prepare", args = {Connection.class, Integer.class})})
public class DynamicMybatisPlugin implements Interceptor {

  /**
   * 正则表达式匹配sql语句，用于修改sql语句中的表名
   * 但是这里只能修改一个表名，也就是说，不支持连表查询；只允许一张表表名的修改
   */
  private Pattern pattern = Pattern.compile("(from|into|update)[\\s]{1,}(\\w{1,})", Pattern.CASE_INSENSITIVE);

  /**
   * 定义拦截规则
   * @param invocation
   * @return
   * @throws Throwable
   */
  @Override
  public Object intercept(Invocation invocation) throws Throwable {
    // 获取StatementHandler
    StatementHandler statementHandler = (StatementHandler) invocation.getTarget();
    MetaObject metaObject = MetaObject.forObject(statementHandler, SystemMetaObject.DEFAULT_OBJECT_FACTORY, SystemMetaObject.DEFAULT_OBJECT_WRAPPER_FACTORY, new DefaultReflectorFactory());
    MappedStatement mappedStatement = (MappedStatement) metaObject.getValue("delegate.mappedStatement");

    // 获取自定义注解判断是否进行分表操作
    String id = mappedStatement.getId();
    String className = id.substring(0, id.lastIndexOf("."));
    Class<?> clazz = Class.forName(className);
    // 反射获取注解
    DBRouterStrategy dbRouterStrategy = clazz.getAnnotation(DBRouterStrategy.class);
    // 不用分表，直接放行
    if (null == dbRouterStrategy || !dbRouterStrategy.splitTable()){
      return invocation.proceed();
    }
    // 开始路由到分表

    // 获取SQL
    BoundSql boundSql = statementHandler.getBoundSql();
    String sql = boundSql.getSql();

    // 先匹配表名字段
    Matcher matcher = pattern.matcher(sql);
    String tableName = null;
    if (matcher.find()) {
      tableName = matcher.group().trim();
    }
    assert null != tableName;
    // 替换表名字段，也就是加上后缀001等
    String replaceSql = matcher.replaceAll(tableName + "_" + DBContextHolder.getTBKey());

    // 通过反射修改SQL语句
    Field field = boundSql.getClass().getDeclaredField("sql");
    field.setAccessible(true);
    // 替换为更新后的sql
    field.set(boundSql, replaceSql);
    field.setAccessible(false);

    //放行sql
    return invocation.proceed();
  }

}