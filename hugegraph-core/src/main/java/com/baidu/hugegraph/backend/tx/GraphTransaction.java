package com.baidu.hugegraph.backend.tx;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Element;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.util.ElementHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.baidu.hugegraph.HugeGraph;
import com.baidu.hugegraph.backend.BackendException;
import com.baidu.hugegraph.backend.id.Id;
import com.baidu.hugegraph.backend.query.ConditionQuery;
import com.baidu.hugegraph.backend.query.Query;
import com.baidu.hugegraph.backend.store.BackendEntry;
import com.baidu.hugegraph.backend.store.BackendStore;
import com.baidu.hugegraph.schema.SchemaManager;
import com.baidu.hugegraph.structure.HugeEdge;
import com.baidu.hugegraph.structure.HugeElement;
import com.baidu.hugegraph.structure.HugeVertex;
import com.baidu.hugegraph.type.HugeTypes;
import com.baidu.hugegraph.type.define.HugeKeys;
import com.baidu.hugegraph.type.schema.VertexLabel;
import com.baidu.hugegraph.util.CollectionUtil;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

public class GraphTransaction extends AbstractTransaction {

    private static final Logger logger = LoggerFactory.getLogger(GraphTransaction.class);

    private IndexTransaction indexTx;

    private Set<HugeVertex> vertexes;

    public GraphTransaction(final HugeGraph graph,
            BackendStore store, BackendStore indexStore) {
        super(graph, store);
        this.indexTx = new IndexTransaction(graph, indexStore);
        this.vertexes = new LinkedHashSet<HugeVertex>();
    }

    @Override
    protected void prepareCommit() {
        // ensure all the target vertexes (of out edges) are in this.vertexes
        for (HugeVertex source : this.vertexes) {
            Iterator<Vertex> targets = source.vertices(Direction.OUT);
            while (targets.hasNext()) {
                HugeVertex target = (HugeVertex) targets.next();
                this.vertexes.add(target);
            }
        }

        // serialize and add into super.additions
        for (HugeVertex v : this.vertexes) {
            this.addEntry(this.serializer.writeVertex(v));
            this.indexTx.updateVertexIndex(v, false);
        }

        this.vertexes.clear();
    }

    @Override
    public void commit() throws BackendException {
        super.commit();
        this.indexTx.commit();
    }

    @Override
    public void rollback() throws BackendException {
        super.rollback();
        this.indexTx.rollback();
    }

    @Override
    public Iterable<BackendEntry> query(Query query) {
        if (query instanceof ConditionQuery) {
            query = this.optimizeQuery((ConditionQuery) query);
        }
        return super.query(query);
    }

    public Vertex addVertex(HugeVertex vertex) {
        this.beforeWrite();
        vertex = this.vertexes.add(vertex) ? vertex : null;
        this.afterWrite();
        return vertex;
    }

    public Vertex addVertex(Object... keyValues) {
        ElementHelper.legalPropertyKeyValueArray(keyValues);

        Id id = HugeVertex.getIdValue(keyValues);
        Object label = HugeVertex.getLabelValue(keyValues);

        // Vertex id must be null now
        if (id != null) {
            String msg = "User defined id of Vertex is not supported";
            throw new IllegalArgumentException(msg);
        }

        if (label == null) {
            // Preconditions.checkArgument(label != null, "Vertex label must be not null");
            throw Element.Exceptions.labelCanNotBeNull();
        } else if (label instanceof String) {
            SchemaManager schema = this.graph.schema();
            label = schema.vertexLabel((String) label);
        }

        // create HugeVertex
        assert (label instanceof VertexLabel);

        // check keyValues whether contain primaryKey in definition of vertexLabel.
        Set<String> primaryKeys = ((VertexLabel) label).primaryKeys();
        Preconditions.checkArgument(CollectionUtil.containsAll(
                ElementHelper.getKeys(keyValues), primaryKeys),
                "The primary key(s) must be setted: " + primaryKeys);

        HugeVertex vertex = new HugeVertex(this.graph, id, (VertexLabel) label);
        // set properties
        ElementHelper.attachProperties(vertex, keyValues);

        // generate an id and assign it if not exists
        if (id == null) {
            vertex.assignId();
        }

        return this.addVertex(vertex);
    }

    public Iterator<Vertex> queryVertices(Object... vertexIds) {
        List<Vertex> list = new ArrayList<Vertex>(vertexIds.length);

        for (Object vertexId : vertexIds) {
            Id id = HugeElement.getIdValue(T.id, vertexId);
            BackendEntry entry = this.get(HugeTypes.VERTEX, id);
            Vertex vertex = this.serializer.readVertex(entry);
            assert vertex != null;
            list.add(vertex);
        }

        return list.iterator();
    }

    public Iterator<Vertex> queryVertices(Query q) {
        List<Vertex> list = new ArrayList<Vertex>();

        Iterator<BackendEntry> entries = this.query(q).iterator();
        while (entries.hasNext()) {
            Vertex vertex = this.serializer.readVertex(entries.next());
            assert vertex != null;
            list.add(vertex);
        }

        return list.iterator();
    }

    public Iterator<Edge> queryEdges(Object... edgeIds) {
        List<Edge> list = new ArrayList<Edge>(edgeIds.length);

        for (Object edgeId : edgeIds) {
            Id id = HugeElement.getIdValue(T.id, edgeId);
            BackendEntry entry = this.get(HugeTypes.EDGE, id);
            Vertex vertex = this.serializer.readVertex(entry);
            assert vertex != null;
            list.addAll(ImmutableList.copyOf(vertex.edges(Direction.BOTH)));
        }

        return list.iterator();
    }

    public Iterator<Edge> queryEdges(Query q) {
        Iterator<Vertex> vertices = this.queryVertices(q);

        Map<Id, Edge> results = new HashMap<>();
        while (vertices.hasNext()) {
            HugeVertex vertex = (HugeVertex) vertices.next();
            for (HugeEdge edge : vertex.getEdges()) {
                if (!results.containsKey(edge.id())) {
                    // NOTE: ensure all edges in results are OUT
                    results.put(edge.id(), edge.switchOutDirection());
                } else {
                    logger.debug("results contains edge: {}", edge);
                }
            }
        }

        return results.values().iterator();
    }

    protected Query optimizeQuery(ConditionQuery query) {
        // optimize vertex query
        Object label = query.condition(HugeKeys.LABEL);
        if (label != null && query.resultType() == HugeTypes.VERTEX) {
            // query vertex by label + primary-values
            Set<String> keys = this.graph.schema().vertexLabel(
                    label.toString()).primaryKeys();
            if (!keys.isEmpty() && query.matchUserpropKeys(keys)) {
                query.eq(HugeKeys.PRIMARY_VALUES, query.userpropValuesString());
                query.resetUserpropConditions();
                logger.debug("query vertices by primaryKeys: {}", query);
                return query;
            }
        }

        // optimize edge query
        if (label != null && query.resultType() == HugeTypes.EDGE) {
            // query edge by sourceVertex + direction + label + sort-values
            Set<String> keys = this.graph.schema().edgeLabel(
                    label.toString()).sortKeys();
            if (query.condition(HugeKeys.SOURCE_VERTEX) != null
                    && query.condition(HugeKeys.DIRECTION) != null
                    && !keys.isEmpty() && query.matchUserpropKeys(keys)) {
                query.eq(HugeKeys.SORT_VALUES, query.userpropValuesString());
                query.resetUserpropConditions();
                logger.debug("query edges by sortKeys: {}", query);
                return query;
            }
        }

        // query only by sysprops, like: vertex label, edge label
        // NOTE: we assume sysprops would be indexed by backend store
        // but we don't support query edges only by direction
        if (query.allSysprop()) {
            if (query.resultType() == HugeTypes.EDGE
                    && query.condition(HugeKeys.DIRECTION) != null
                    && query.condition(HugeKeys.SOURCE_VERTEX) == null) {
                String msg = "Not support query edges only by direction";
                throw new BackendException(msg);
            }
            return query;
        }

        // optimize by index-query
        return this.indexTx.query(query);
    }
}