package pers.yozora7.lanfirewallmgr.service;

import pers.yozora7.lanfirewallmgr.parser.data.Address;
import pers.yozora7.lanfirewallmgr.parser.data.Rule;
import pers.yozora7.lanfirewallmgr.parser.data.Service;

public abstract class ConfDaoService {
    public abstract int address(Address data);
    public abstract int service(Service data);
    public abstract void serviceGroup(String service, String group);
    public abstract int rule(Rule data);
    public abstract boolean isServiceGroup(String group);
}
