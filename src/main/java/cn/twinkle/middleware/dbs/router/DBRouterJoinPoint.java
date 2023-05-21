package cn.twinkle.middleware.dbs.router;

import cn.twinkle.middleware.dbs.router.annotation.DBRouter;
import cn.twinkle.middleware.dbs.router.strategy.IDBRouterStrategy;
import java.lang.reflect.Method;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Author: zhencym
 * @DATE: 2023/4/23
 * 数据路由切面，通过自定义注解的方式，拦截被切面的方法，进行数据库路由
 */
@Aspect // 切面类
public class DBRouterJoinPoint {

  private Logger logger = LoggerFactory.getLogger(DBRouterJoinPoint.class);

  private DBRouterConfig dbRouterConfig;

  private IDBRouterStrategy dbRouterStrategy;

  public DBRouterJoinPoint(DBRouterConfig dbRouterConfig, IDBRouterStrategy dbRouterStrategy) {
    this.dbRouterConfig = dbRouterConfig;
    this.dbRouterStrategy = dbRouterStrategy;
  }

  /**
   * 用一个函数名，代替注解的全类名，作为切入点
   */
  @Pointcut("@annotation(cn.twinkle.middleware.dbs.router.annotation.DBRouter)")
  public void aopPoint() {
  }

  /**
   * 所有需要分库分表的操作，都需要使用自定义注解进行拦截，拦截后读取方法中的入参字段，根据字段进行路由操作。
   * 1. dbRouter.key() 确定根据哪个字段进行路由
   * 2. getAttrValue 根据数据库路由字段，从入参中读取出对应的值。比如路由 key 是 uId，那么就从入参对象 Obj 中获取到 uId 的值。
   * 3. dbRouterStrategy.doRouter(dbKeyAttr) 路由策略根据具体的路由值进行处理
   * 4. 路由处理完成比，就是放行。 jp.proceed();
   * 5. 最后 dbRouterStrategy 需要执行 clear 因为这里用到了 ThreadLocal 需要手动清空。关于 ThreadLocal 内存泄漏介绍 https://t.zsxq.com/027QF2fae
   */
  @Around("aopPoint() && @annotation(dbRouter)")
  public Object doRouter(ProceedingJoinPoint jp, DBRouter dbRouter) throws Throwable {
    // 获取注解参数key
    String dbKey = dbRouter.key();
    // 当注解中没有传入路由参数，那就从配置中读取，如果配置也为空，那就返回错误
    if (StringUtils.isEmpty(dbKey) && StringUtils.isEmpty(dbRouterConfig.getRouterKey())) {
      throw new RuntimeException("annotation DBRouter key is null！");
    }
    // 注解配置、默认配置中有一个路由参数不为空：注解配置优先级高
    dbKey = StringUtils.isNotBlank(dbKey) ? dbKey : dbRouterConfig.getRouterKey();
    // 路由属性，从入参对象中获取键值，注意是从入参对象中获取到(实际上传来的是多个键对值)
    String dbKeyAttr = getAttrValue(dbKey, jp.getArgs());
    // 路由策略
    dbRouterStrategy.doRouter(dbKeyAttr);
    logger.info("本次路由结果：库id：{} 表id：{}", dbRouterStrategy.getDBKey(), dbRouterStrategy.getTBKey());
    // 返回结果
    try {

      //对于环绕通知，在这里放行，目标方法就在proceed前执行（相当于替换proceed的位置执行目标方法）

      return jp.proceed();
    } finally {
      dbRouterStrategy.clear();
    }
  }

  /**
   * 获取切入点的方法
   * @param jp
   * @return
   * @throws NoSuchMethodException
   */
  public Method getMethod(JoinPoint jp) throws NoSuchMethodException {
    Signature sig = jp.getSignature();
    MethodSignature methodSignature = (MethodSignature) sig;
    // 先获取目标对象，在获取目标对象的类对象class,然后通过反射获取方法；（根据签名方法名、签名方法参数）
    return jp.getTarget().getClass().getMethod(methodSignature.getName(), methodSignature.getParameterTypes());
  }

  /**
   * 获取键值对应的属性
   * @param attr
   * @param args
   * @return
   */
  public String getAttrValue(String attr, Object[] args) {
    // 长度为1，切入方法传参对象只有一个属性，那就直接返回
    if (1 == args.length) {
      Object arg = args[0];
      if (arg instanceof String) {
        return arg.toString();
      }
    }

    // 切入方法有多个 键对值参数，遍历查找返回；（需要遍历找出key为arr的那一个value）
    String filedValue = null;
    for (Object arg : args) {
      try {
        if (StringUtils.isNotBlank(filedValue)) {
          break;
        }
        // 根据key找value
        filedValue = BeanUtils.getProperty(arg, attr);
      } catch (Exception e) {
        logger.error("获取路由属性值失败 attr：{}", attr, e);
      }
    }
    return filedValue;
  }

}
