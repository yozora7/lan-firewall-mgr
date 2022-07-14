package pers.yozora7.lanfirewallmgr.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

/**
 * 规则实体类
 */
public class Rule {
    // 序号
    private int id;
    // 名称
    private String name;
    // 源端安全域
    private String srcZoneIds;
    // 源端地址集列表 (source-address address-set ...)
    private String srcSetIds;
    // 源端IP地址列表
    private String srcNetIds;
    // 目标端安全域
    private String dstZoneIds;
    // 目标端地址集列表
    private String dstSetIds;
    // 目标端IP地址列表
    private String dstNetIds;
    // 服务列表
    private String serviceIds;
    // service-group
    private String serviceGroups;
    // action
    private String action;
}
