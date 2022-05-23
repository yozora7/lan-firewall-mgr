package pers.yozora7.lanfirewallmgr.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Net {
    private long id;
    // 起始地址
    private String start;
    // 结束地址
    private String end;
    // 地址集
    private int setId;
}
