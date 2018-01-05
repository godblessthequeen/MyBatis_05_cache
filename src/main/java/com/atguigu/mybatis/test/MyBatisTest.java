package com.atguigu.mybatis.test;

import com.atguigu.mybatis.beans.Department;
import com.atguigu.mybatis.beans.Employee;
import com.atguigu.mybatis.dao.DepartmentMapper;
import com.atguigu.mybatis.dao.EmployeeMapper;
import com.atguigu.mybatis.dao.EmployeeMapperDynamicSQL;
import com.atguigu.mybatis.dao.EmployeeMapperPlus;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author lenovo on 2018/1/2.
 * @version 1.0
 */

/**
 * 1.接口式编程
 *  原生：     Dao     ----->     DaoImpl
 *  mybatis：  Mapper  ----->     xxMapper.xml
 *
 * 2.SqlSession代表和数据库的一次会话  用完必须关闭
 * 3.SqlSession和connection一样都是非线程安全的，每次使用都应该去获取新的对象
 * 4.mapper接口没有实现类，但是mybatis会为这个接口生成一个代理对象
 *      （将接口与xml进行绑定）
 *      EmployeeMapper empMapper = sqlSession.getMapper(EmployeeMapper.class)
 * 5.两个重要的配置文件：
 *      mybatis的全局配置文件：包含数据库连接池信息，事务管理器信息等...系统运行环境信息
 *      sql映射文件：保存了每一个sql语句的映射信息：将sql抽取出来。
 */
public class MyBatisTest {

    public SqlSessionFactory getSqlSessionFactory() throws IOException {
        String resources = "mybatis-config.xml";
        InputStream inputStream = Resources.getResourceAsStream(resources);
        return new SqlSessionFactoryBuilder().build(inputStream);
    }

    /**
     * 两级缓存：
     * 一级缓存：（本地缓存）：sqlSession级别的缓存。一级缓存是一直开启的；SqlSession级别的一个Map
     *      与数据库同一次会话期间查询到的数据会放在本地缓存中。
     *      以后如果需要获取相同的数据，直接从缓存中拿，没必要再去查询数据库；
     *
     *      一级缓存失效情况（没有使用到当前一级缓存的情况，效果就是，还需要向数据库发出查询）
     *      1.sqlSession不同。
     *      2.sqlSession相同，查询条件不同（当前一级缓存中还没有这个数据）
     *      3.sqlSession相同，两次查询之间执行了增删改（这次增删改可能对当前数据有影响）
     *      4.sqlSession相同，手动清除了一级缓存（缓存清空）
     * 二级缓存：（全局缓存）：基于namespace级别的缓存：一个namespace对应一个二级缓存：
     *      工作机制：
     *      1.一个会话，查询一条数据，这个就会被放在当前会话的一级缓存中
     *      2.如果会话关闭；一级缓存中的数据会被保存到二级缓存中；新的会话查询信息，就可以参照二级缓存中的内容
     *      3.sqlSession===EmployeeMapper===>Employee
     *                      DepartmentMapper===>Department
     *         不同namespace查出的数据会放在自己对应的缓存中（map）
     *         效果：数据会从二级缓存中获取
     *               查出的数据都会被默认先放在一级缓存中。
     *               只有会话提交或者关闭以后，一级缓存中的数据才会转移到二级缓存中
     *
     *      使用：
     *          1）.开启全局二级缓存配置：<setting name="cacheEnable" value="true"/>
     *          2）.去mapper.xml中配置使用而二级缓存：
     *              <cache/>
     *          3）.我们的POJO需要实现序列化接口
     *
     *      和缓存有关的设置/属性：
     *          1）.cahceEnabled=true   false:关闭缓存（二级缓存关闭，一级缓存一直可用的）
     *          2）.每个select标签都有userCache="true"  false:不使用缓存（一级缓存依然可用，二级缓存不使用）
     *          3）.每个增删改标签的：flushCache="true"：【（一级二级都会清除）】
     *                                增删改执行完成后就会清除缓存；
     *                                测试：flushCache="true"，一级缓存就清空了；二级缓存也会被清除
     *                                查询标签：flushCache="false",如果改为true，每次查询之前都会清空缓存，缓存
     *                                          是没有被使用的
     *          4）.sqlSession.clearCache()：只是清除当前session的一级缓存
     *          5）.localCacheScope：本地缓存作用域：（以及缓存session）；当前会话的所有数据保存在会话缓存中
     *                               statement：可以禁用一级缓存
     *
     *  第三方缓存整合：
     *      1）.导入第三方缓存包即可
     *      2）.导入与第三方缓存整合的适配包，官方有
     *      3）.mapper.xml中使用自定义缓存
     *          <cache type="org.mybatis.caches.ehcache.EhcacheCache"></cache>
     */

    @Test
    public void testSecondLevelCache2() throws IOException {
        SqlSessionFactory sqlSessionFactory = getSqlSessionFactory();
        SqlSession sqlSession = sqlSessionFactory.openSession();
        SqlSession sqlSession2 = sqlSessionFactory.openSession();

        try {
            //1.
            DepartmentMapper mapper = sqlSession.getMapper(DepartmentMapper.class);
            DepartmentMapper mapper2 = sqlSession2.getMapper(DepartmentMapper.class);

            Department dept = mapper.getDeptById(1);
            System.out.println(dept);
            sqlSession.close();

            //第二次查询时从二级缓存中拿到的数据，并没有发送新的sql
            Department dept2 = mapper2.getDeptById(1);
            System.out.println(dept2);
            sqlSession2.close();

        }finally {

        }
    }

    @Test
    public void testSecondLevelCache() throws IOException {
        SqlSessionFactory sqlSessionFactory = getSqlSessionFactory();
        SqlSession sqlSession = sqlSessionFactory.openSession();
        SqlSession sqlSession2 = sqlSessionFactory.openSession();

        try {
            //1.
            EmployeeMapper mapper = sqlSession.getMapper(EmployeeMapper.class);
            EmployeeMapper mapper2 = sqlSession2.getMapper(EmployeeMapper.class);

            Employee employee = mapper.getEmpById(1);
            System.out.println(employee);
            sqlSession.close();

            //第二次查询时从二级缓存中拿到的数据，并没有发送新的sql
            Employee employee2 = mapper2.getEmpById(1);
            System.out.println(employee2);
            sqlSession2.close();

        }finally {

        }
    }

    @Test
    public void testFirstLevelCache() throws IOException {
        SqlSessionFactory sqlSessionFactory = getSqlSessionFactory();
        SqlSession sqlSession = sqlSessionFactory.openSession();

        try{
            EmployeeMapper mapper = sqlSession.getMapper(EmployeeMapper.class);
            Employee emp01 = mapper.getEmpById(1);
            System.out.println(emp01);

            //1.sqlSession不同。
//            SqlSession sqlSession2 = sqlSessionFactory.openSession();
//            EmployeeMapper mapper2 = sqlSession2.getMapper(EmployeeMapper.class);

            //2.sqlSession相同，查询条件不同

            //3.sqlSession相同，两次查询之间执行了增删改（这次增删改可能对当前数据有影响）
//            mapper.addEmp(new Employee(null,"curry","curry@atguigu.com","1"));

            //4.sqlSession相同，手动清除了一级缓存（缓存清空）
            sqlSession.clearCache();

            Employee emp02 = mapper.getEmpById(1);
            System.out.println(emp02);
            System.out.println(emp02==emp01);

//            sqlSession2.close();
        }finally {
            sqlSession.close();
        }
    }
}
