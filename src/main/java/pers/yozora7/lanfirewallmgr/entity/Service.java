package pers.yozora7.lanfirewallmgr.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Service {
    // 服务名称
    private String name;
    // 序号
    private int id;
    // 协议 (TCP/UDP)
    private String protocol;
    // group
    private String group;
    // 源端口
    private int srcStartPort;
    private int srcEndPort;
    // 目标端口
    private int dstStartPort;
    private int dstEndPort;
}
