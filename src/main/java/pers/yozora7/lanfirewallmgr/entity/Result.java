package pers.yozora7.lanfirewallmgr.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Result {
    private int ruleId;
    private String ruleName;
    private String srcZone;
    private String dstZone;
    private String srcNetIds;
    private String srcSetIds;
    private String serviceIds;
    private String action;
}
