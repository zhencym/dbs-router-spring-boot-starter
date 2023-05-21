package cn.twinkle.middleware.test;

import cn.twinkle.middleware.dbs.router.annotation.DBRouter;

/**
 * @Author: zhencym
 * @DATE: 2023/4/23
 * 测试接口
 */

public interface IUserDao {

    @DBRouter(key = "userId")
    void insertUser(String req);

}
