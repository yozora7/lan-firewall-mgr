package pers.yozora7.lanfirewallmgr.service;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import pers.yozora7.lanfirewallmgr.parser.NetUtils;

import java.util.List;

public class OrientUtils {
    public static boolean hasFirewall (GraphTraversalSource g, String name) {
        return !g.V().hasLabel("firewall").has("name", name).toList().isEmpty();
    }

    public static boolean hasZone (GraphTraversalSource g, String name) {
        return !g.V().hasLabel("zone").has("name", name).toList().isEmpty();
    }

    public static boolean hasSubnet (GraphTraversalSource g, String address) {
        if (g.V().hasLabel("subnet").has("address", address).toList().isEmpty()) {
            return false;
        }
        List<String> list = g.V().hasLabel("subnet").<String>values("address").toList();
        // 输入具体IP地址
        if (address.indexOf("/") == -1) {
            for (String i : list) {
                if (i.indexOf("/") != -1 && NetUtils.isIpInSubnet(address, i)) {
                    return true;
                }
                else if (address == i){
                    return true;
                }
            }
        }
        // 输入CIDR网段
        else {
            for (String i : list) {
                if (i.indexOf("/") != -1 && NetUtils.isSubnetInSubnet(address, i)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean hasEdge(GraphTraversalSource g ,Vertex a, Vertex b, String label) {
        return !g.V(a).outE(label).as("e").inV().is(b).toList().isEmpty();
    }
}
