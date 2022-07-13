package pers.yozora7.lanfirewallmgr.mybatis;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ZoneDao {
    void createTable(@Param("db") String db, @Param("name") String name);
    int queryId(@Param("db") String db, @Param("name") String name);
    int count(String db);
    int insert(@Param("db") String db, @Param("name") String name);
}
