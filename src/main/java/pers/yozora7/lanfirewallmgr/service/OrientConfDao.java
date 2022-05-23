package pers.yozora7.lanfirewallmgr.service;

import com.orientechnologies.orient.core.db.document.ODatabaseDocument;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.tinkerpop.gremlin.orientdb.OrientGraph;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import pers.yozora7.lanfirewallmgr.parser.data.Address;
import pers.yozora7.lanfirewallmgr.parser.data.Rule;
import pers.yozora7.lanfirewallmgr.parser.data.Service;

import java.util.List;

@Slf4j
@Setter
@Getter
public class OrientConfDao extends ConfDao {

    private OrientGraph graph;

    @Override
    public long address(Address data) {
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
                    .property("id", data.getId())
                    .property("start", data.getStart())
                    .property("end", data.getEnd())
                    .property("set", data.getSet())
                    .next();
            log.info("insert address: id = {}", (Object) address.value("id"));
            return address.value("id");
        }
    }

    @Override
    public long service(Service data) {
        GraphTraversalSource g = graph.traversal();
        List<Vertex> exists = g.V().hasLabel("service").has("name", data.getName()).toList();
        if (!exists.isEmpty()) {
            return exists.get(0).value("id");
        }
        else {
            Vertex service = g.addV("service")
                    .property("id", data.getId())
                    .property("name", data.getName())
                    .property("protocol", data.getProtocol())
                    .property("srcStartPort", data.getSrcStartPort())
                    .property("srcEndPort", data.getSrcEndPort())
                    .property("dstStartPort", data.getDstStartPort())
                    .property("dstEndPort", data.getDstEndPort())
                    .property("group", data.getGroup())
                    .next();
            log.info("insert service: id = {}", (Object) service.value("id"));
            return service.value("id");
        }
    }

    @Override
    public void serviceGroup(String serviceName, String groupName) {
        GraphTraversalSource g = graph.traversal();
        List<Vertex> services = g.V()
                .hasLabel("service")
                .has("name", serviceName)
                .toList();
        if (!services.isEmpty()) {
            g.V(services.get(0).id()).property("group",groupName);
        }
    }

    @Override
    public long rule(Rule data) {
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
                    .property("id", data.getId())
                    .property("name", data.getName())
                    .property("srcZone", data.getSrcZone())
                    .property("dstZone", data.getDstZone())
                    .property("action", data.getAction())
                    .next();
            // source-address address-set
            for (String i : data.getSrcSets()) {
                List<Vertex> addresses = g.V().hasLabel("address").has("set", i).toList();
                for (Vertex j : addresses) {
                    j.addEdge("src-set", rule);
                }
            }
            // destination-address address-set
            for (String i : data.getDstSets()) {
                List<Vertex> addresses = g.V().hasLabel("address").has("set", i).toList();
                for (Vertex j : addresses) {
                    rule.addEdge("dst-set", j);
                }
            }
            // source-address
            for (long i : data.getSrcAddressIds()) {
                Vertex address = g.V().hasLabel("address").has("id", i).next();
                address.addEdge("src", rule);
            }
            // destination-address
            for (long i : data.getDstAddressIds()) {
                Vertex address = g.V().hasLabel("address").has("id", i).next();
                rule.addEdge("dst", address);
            }
            // service
            for (long i : data.getServiceIds()) {
                Vertex service = g.V().hasLabel("service").has("id", i).next();
                service.addEdge("apply", rule);
            }
            // service type group
            for (String i : data.getServiceGroups()) {
                List<Vertex> services = g.V().hasLabel("service").has("name", i).toList();
                for (Vertex j : services) {
                    j.addEdge("apply", rule);
                }
            }
            log.info("insert rule: id = {}", (Object) rule.value("id"));
            return rule.value("id");
        }
    }

    @Override
    public boolean isServiceGroup(String group) {
        GraphTraversalSource g = graph.traversal();
        List<Vertex> groups = g.V().hasLabel("service").has("group", group).toList();
        return !groups.isEmpty();
    }

    @Override
    public long countIds(String className) {
        ODatabaseDocument db = graph.database();
        return db.getClass(className).count();
    }
}