package pers.yozora7.lanfirewallmgr.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * IP地址实体类
 * start/startMask: 起始地址/子网掩码
 * end/endMask: 结束地址/子网掩码
 * setId: 所属地址集(set)的ID
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Net {
    private int id;
    // 起始地址
    private String start;
    private int startMask;
    // 结束地址
    private String end;
    private int endMask;
    // 地址集
    private int setId;
}
