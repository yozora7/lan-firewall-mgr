package pers.yozora7.lanfirewallmgr.parser.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Rule {
    // 序号
    private long id;
    // 名称
    private String name;
    // 源端安全域
    private String srcZone;
    // 源端地址集列表 (source-address address-set ...)
    private Set<String> srcSets;
    // 源端IP地址列表
    private Set<Long> srcAddressIds;
    // 目标端安全域
    private String dstZone;
    // 目标端地址集列表
    private Set<String> dstSets;
    // 目标端IP地址列表
    private Set<Long> dstAddressIds;
    // 服务列表
    private Set<Long> serviceIds;
    // service-group
    private Set<String> serviceGroups;
    // action
    private String action;
}
