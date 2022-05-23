package pers.yozora7.lanfirewallmgr.service;

import pers.yozora7.lanfirewallmgr.parser.data.Address;
import pers.yozora7.lanfirewallmgr.parser.data.Rule;
import pers.yozora7.lanfirewallmgr.parser.data.Service;

public abstract class ConfDao {
    public abstract long address(Address data);
    public abstract long service(Service data);
    public abstract void serviceGroup(String service, String group);
    public abstract long rule(Rule data);
    public abstract boolean isServiceGroup(String group);
    public abstract long countIds(String className);
}
