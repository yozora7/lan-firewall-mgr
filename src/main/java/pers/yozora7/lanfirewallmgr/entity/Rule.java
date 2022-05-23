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
public class Rule {
    // 序号
    private int id;
    // 名称
    private String name;
    // 源端安全域
    private String srcZone;
    // 源端地址集列表 (source-address address-set ...)
    private HashSet<Integer> srcSetIds;
    // 源端IP地址列表
    private HashSet<Integer> srcNetIds;
    // 目标端安全域
    private String dstZone;
    // 目标端地址集列表
    private HashSet<Integer> dstSetIds;
    // 目标端IP地址列表
    private HashSet<Integer> dstNetIds;
    // 服务列表
    private HashSet<Integer> serviceIds;
    // service-group
    private HashSet<String> serviceGroups;
    // action
    private String action;
}
