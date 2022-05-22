package pers.yozora7.lanfirewallmgr.service;

import lombok.Getter;
import lombok.Setter;
import org.apache.tinkerpop.gremlin.orientdb.OrientGraph;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import pers.yozora7.lanfirewallmgr.parser.data.Address;
import pers.yozora7.lanfirewallmgr.parser.data.Rule;
import pers.yozora7.lanfirewallmgr.parser.data.Service;

import java.util.List;

@Setter
@Getter
public class OrientConfService extends ConfDaoService {

    private OrientGraph graph;

    @Override
    public int address(Address data) {
        GraphTraversalSource g = graph.traversal();
        List<Vertex> exists = g.V()
                .hasLabel("address")
                .has("start", data.getStart())
                .has("end", data.getEnd())
                .has("set", data.getSet())
                .toList();
        if (!exists.isEmpty()) {
            return exists.get(0).value("id");
        }
        else {
            Vertex address = g.addV("address")
                    .property("start", data.getStart())
                    .property("end", data.getEnd())
                    .property("set", data.getSet())
                    .next();
            return address.value("id");
        }
    }

    @Override
    public int service(Service data) {
        GraphTraversalSource g = graph.traversal();
        List<Vertex> exists = g.V()
                .hasLabel("service")
                .has("name", data.getName())
                .property("protocol", data.getProtocol())
                .property("srcStartPort", data.getSrcStartPort())
                .property("srcEndPort", data.getSrcEndPort())
                .property("dstStartPort", data.getDstStartPort())
                .property("dstEndPort", data.getDstEndPort())
                .toList();
        if (!exists.isEmpty()) {
            return exists.get(0).value("id");
        }
        else {
            Vertex service = g.addV("service")
                    .property("name", data.getName())
                    .property("protocol", data.getProtocol())
                    .property("srcStartPort", data.getSrcStartPort())
                    .property("srcEndPort", data.getSrcEndPort())
                    .property("dstStartPort", data.getDstStartPort())
                    .property("dstEndPort", data.getDstEndPort())
                    .property("group", data.getGroup())
                    .next();
            return service.value("id");
        }
    }

    @Override
    public void serviceGroup(String service, String group) {
        // TODO
        
    }

    @Override
    public int rule(Rule data) {
        GraphTraversalSource g = graph.traversal();
        List<Vertex> exists = g.V()
                .hasLabel("rule")
                .has("name", data.getName())
                .toList();
        if (!exists.isEmpty()) {
            return exists.get(0).value("id");
        }
        else {
            Vertex rule = g.addV("rule")
                    .property("name", data.getName())
                    .property("srcZone", data.getSrcZone())
                    .property("dstZone", data.getDstZone())
                    .next();
            // source-address address-set
            for (String i : data.getSrcSets()) {
                List<Vertex> addresses = g.V("address").has("set", i).toList();
                for (Vertex j : addresses) {
                    j.addEdge("src-set", rule);
                }
            }
            // destination-address address-set
            for (String i : data.getDstSets()) {
                List<Vertex> addresses = g.V("address").has("set", i).toList();
                for (Vertex j : addresses) {
                    rule.addEdge("dst-set", j);
                }
            }
            // source-address
            for (int i : data.getSrcAddressIds()) {
                Vertex address = g.V("address").has("id", i).next();
                address.addEdge("src", rule);
            }
            // source-address
            for (int i : data.getSrcAddressIds()) {
                Vertex address = g.V("address").has("id", i).next();
                address.addEdge("src", rule);
            }
            // destination-address
            for (int i : data.getDstAddressIds()) {
                Vertex address = g.V("address").has("id", i).next();
                rule.addEdge("dst", address);
            }
            // service
            for (int i : data.getServiceIds()) {
                Vertex service = g.V("service").has("id", i).next();
                service.addEdge("apply-to", rule);
            }
            // TODO: service type group
            return rule.value("id");
        }
    }

    @Override
    public boolean isServiceGroup(String group) {
        GraphTraversalSource g = graph.traversal();
        List<Vertex> groups = g.V("service").has("group", group).toList();
        return !groups.isEmpty();
    }
}