package pers.yozora7.lanfirewallmgr.mybatis;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import pers.yozora7.lanfirewallmgr.entity.Net;

@Mapper
public interface NetDao {
    void createDatabase(String db);
    void createTable(@Param("db") String db, @Param("net") Net net);
    int queryId(@Param("db") String db, @Param("net") Net net);
    int count(String db);
    int insert(@Param("db") String db, @Param("net") Net net);
}
