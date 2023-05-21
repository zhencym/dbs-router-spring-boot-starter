package cn.twinkle.middleware.dbs.router.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @Author: zhencym
 * @DATE: 2023/4/23
 * 二、
 * 路由注解
 * 开发注解的三个标签：
 * @Documented、@Retention、@Target
 * 注解参数后接（） + 默认值
 */

@Documented //可将java doc注释展示
@Retention(RetentionPolicy.RUNTIME) // 标注接口作用范围(运行时.编译时等)
@Target({ElementType.TYPE, ElementType.METHOD}) //该接口可在类、接口、方法上声明
public @interface DBRouter {

  /**
   * 分库分表字段
   * 字段默认值为空，即从所有库中查找、插入
   */
  String key() default "";

}
