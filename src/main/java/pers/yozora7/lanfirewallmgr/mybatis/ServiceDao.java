package pers.yozora7.lanfirewallmgr.mybatis;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import pers.yozora7.lanfirewallmgr.entity.Service;

@Mapper
public interface ServiceDao {
    void createTable(@Param("db") String db, @Param("service") Service service);
    int queryId(@Param("db") String db, @Param("service") Service service);
    int count(String db);
    int insert(@Param("db") String db, @Param("service") Service service);
    void updateGroup(@Param("db") String db, @Param("group") String group, @Param("name") String name);
}
