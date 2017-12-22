/*
 * Copyright 2017 HugeGraph Authors
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.baidu.hugegraph.core;

import java.util.List;

import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.junit.Assume;
import org.junit.Test;

import com.baidu.hugegraph.HugeException;
import com.baidu.hugegraph.HugeGraph;
import com.baidu.hugegraph.exception.NotFoundException;
import com.baidu.hugegraph.schema.SchemaManager;
import com.baidu.hugegraph.schema.VertexLabel;
import com.baidu.hugegraph.testutil.Assert;
import com.baidu.hugegraph.type.define.IdStrategy;

public class VertexLabelCoreTest extends SchemaCoreTest {

    @Test
    public void testAddVertexLabel() {
        super.initPropertyKeys();
        SchemaManager schema = graph().schema();

        VertexLabel person = schema.vertexLabel("person")
                             .properties("name", "age", "city")
                             .primaryKeys("name")
                             .create();

        Assert.assertNotNull(person);
        Assert.assertEquals("person", person.name());
        Assert.assertEquals(3, person.properties().size());
        assertContainsPk(person.properties(), "name", "age", "city");
        Assert.assertEquals(1, person.primaryKeys().size());
        assertContainsPk(person.primaryKeys(), "name");
    }

    @Test
    public void testAddVertexLabelWithIllegalName() {
        SchemaManager schema = graph().schema();

        // Empty string
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            schema.vertexLabel("").create();
        });
        // One space
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            schema.vertexLabel(" ").create();
        });
        // Two spaces
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            schema.vertexLabel("  ").create();
        });
        // Multi spaces
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            schema.vertexLabel("    ").create();
        });
        // Start with '~'
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            schema.vertexLabel("~").create();
        });
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            schema.vertexLabel("~ ").create();
        });
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            schema.vertexLabel("~x").create();
        });
    }

    @Test
    public void testAddVertexLabelWithIdStrategy() {
        super.initPropertyKeys();
        SchemaManager schema = graph().schema();

        VertexLabel person = schema.vertexLabel("person")
                             .properties("name", "age", "city")
                             .create();
        Assert.assertEquals(IdStrategy.AUTOMATIC, person.idStrategy());

        VertexLabel person1 = schema.vertexLabel("person1")
                              .useAutomaticId()
                              .properties("name", "age", "city")
                              .create();
        Assert.assertEquals(IdStrategy.AUTOMATIC, person1.idStrategy());

        VertexLabel person2 = schema.vertexLabel("person2")
                              .useCustomizeId()
                              .properties("name", "age", "city")
                              .create();
        Assert.assertEquals(IdStrategy.CUSTOMIZE, person2.idStrategy());

        VertexLabel person3 = schema.vertexLabel("person3")
                              .properties("name", "age", "city")
                              .primaryKeys("name")
                              .create();
        Assert.assertEquals(IdStrategy.PRIMARY_KEY, person3.idStrategy());

        VertexLabel person4 = schema.vertexLabel("person4")
                              .usePrimaryKeyId()
                              .properties("name", "age", "city")
                              .primaryKeys("name")
                              .create();
        Assert.assertEquals(IdStrategy.PRIMARY_KEY, person4.idStrategy());
    }

    @Test
    public void testAddVertexLabelWithNonPKIdStrategyWithoutProperty() {
        HugeGraph graph = graph();
        SchemaManager schema = graph.schema();

        VertexLabel person1 = schema.vertexLabel("person1")
                              .useAutomaticId()
                              .create();
        Assert.assertEquals(IdStrategy.AUTOMATIC, person1.idStrategy());
        Assert.assertTrue(person1.properties().isEmpty());

        VertexLabel person2 = schema.vertexLabel("person2")
                              .useCustomizeId()
                              .create();
        Assert.assertEquals(IdStrategy.CUSTOMIZE, person2.idStrategy());
        Assert.assertTrue(person2.properties().isEmpty());
    }

    @Test
    public void testAddVertexLabelWithMultiIdStrategy() {
        super.initPropertyKeys();
        SchemaManager schema = graph().schema();

        Assert.assertThrows(IllegalArgumentException.class, () -> {
            schema.vertexLabel("person")
                  .useCustomizeId()
                  .useAutomaticId()
                  .properties("name", "age", "city")
                  .create();
        });

        Assert.assertThrows(IllegalArgumentException.class, () -> {
            schema.vertexLabel("person")
                  .useCustomizeId()
                  .usePrimaryKeyId()
                  .properties("name", "age", "city")
                  .create();
        });

        Assert.assertThrows(IllegalArgumentException.class, () -> {
            schema.vertexLabel("person")
                  .usePrimaryKeyId()
                  .useAutomaticId()
                  .properties("name", "age", "city")
                  .create();
        });
    }

    @Test
    public void testAddVertexLabelWithoutPKStrategyButCallPrimaryKey() {
        super.initPropertyKeys();
        SchemaManager schema = graph().schema();

        Assert.assertThrows(IllegalArgumentException.class, () -> {
            schema.vertexLabel("person")
                  .useCustomizeId()
                  .primaryKeys("name")
                  .properties("name", "age", "city")
                  .create();
        });

        Assert.assertThrows(IllegalArgumentException.class, () -> {
            schema.vertexLabel("person")
                  .useCustomizeId()
                  .primaryKeys("name")
                  .properties("name", "age", "city")
                  .create();
        });
    }

    @Test
    public void testAddVertexWithDefaultIdStrategyAndPassedPk() {
        super.initPropertyKeys();
        HugeGraph graph = graph();
        SchemaManager schema = graph.schema();

        VertexLabel person = schema.vertexLabel("person")
                             .properties("name", "age")
                             .primaryKeys("name")
                             .create();
        Assert.assertEquals(IdStrategy.PRIMARY_KEY, person.idStrategy());
    }

    @Test
    public void testAddVertexWithDefaultIdStrategyAndNotPassedPk() {
        super.initPropertyKeys();
        HugeGraph graph = graph();
        SchemaManager schema = graph.schema();

        VertexLabel person = schema.vertexLabel("person")
                             .properties("name", "age")
                             .create();
        Assert.assertEquals(IdStrategy.AUTOMATIC, person.idStrategy());
    }

    @Test
    public void testAddVertexWithAutomaticIdStrategyButPassedPk() {
        HugeGraph graph = graph();
        SchemaManager schema = graph.schema();

        Assert.assertThrows(IllegalArgumentException.class, () -> {
            schema.vertexLabel("person")
                  .useAutomaticId()
                  .properties("name", "age")
                  .primaryKeys("name")
                  .create();
        });
    }

    @Test
    public void testAddVertexWithAutomaticIdStrategyAndNotPassedPk() {
        super.initPropertyKeys();
        HugeGraph graph = graph();
        SchemaManager schema = graph.schema();

        VertexLabel person = schema.vertexLabel("person")
                             .useAutomaticId()
                             .properties("name", "age")
                             .create();
        Assert.assertEquals(IdStrategy.AUTOMATIC, person.idStrategy());
    }

    @Test
    public void testAddVertexWithCustomizeIdStrategyButPassedPk() {
        super.initPropertyKeys();
        HugeGraph graph = graph();
        SchemaManager schema = graph.schema();

        Assert.assertThrows(IllegalArgumentException.class, () -> {
            schema.vertexLabel("person")
                  .useCustomizeId()
                  .properties("name", "age")
                  .primaryKeys("name")
                  .create();
        });
    }

    @Test
    public void testAddVertexWithCustomizeIdStrategyAndNotPassedPk() {
        super.initPropertyKeys();
        HugeGraph graph = graph();
        SchemaManager schema = graph.schema();

        VertexLabel person = schema.vertexLabel("person")
                             .useCustomizeId()
                             .properties("name", "age")
                             .create();
        Assert.assertEquals(IdStrategy.CUSTOMIZE, person.idStrategy());
    }

    @Test
    public void testAddVertexWithPrimaryKeyIdStrategyAndPassedPk() {
        super.initPropertyKeys();
        HugeGraph graph = graph();
        SchemaManager schema = graph.schema();

        VertexLabel person = schema.vertexLabel("person")
                             .usePrimaryKeyId()
                             .properties("name", "age")
                             .primaryKeys("name")
                             .create();
        Assert.assertEquals(IdStrategy.PRIMARY_KEY, person.idStrategy());
    }

    @Test
    public void testAddVertexWithPrimaryKeyIdStrategyButNotPassedPk() {
        super.initPropertyKeys();
        HugeGraph graph = graph();
        SchemaManager schema = graph.schema();
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            schema.vertexLabel("person")
                  .usePrimaryKeyId()
                  .properties("name", "age")
                  .create();
        });
    }

    @Test
    public void testAddVertexLabelWith2PrimaryKey() {
        super.initPropertyKeys();
        SchemaManager schema = graph().schema();

        VertexLabel person = schema.vertexLabel("person")
                             .properties("name", "age", "city")
                             .primaryKeys("name", "age")
                             .create();

        Assert.assertNotNull(person);
        Assert.assertEquals("person", person.name());
        Assert.assertEquals(3, person.properties().size());
        assertContainsPk(person.properties(), "name", "age", "city");
        Assert.assertEquals(2, person.primaryKeys().size());
        assertContainsPk(person.primaryKeys(), "name", "age");
    }

    @Test
    public void testAddVertexLabelWithPrimaryKeyAssignedMultiTimes() {
        super.initPropertyKeys();
        SchemaManager schema = graph().schema();

        Assert.assertThrows(IllegalArgumentException.class, () -> {
            schema.vertexLabel("person")
                  .properties("name", "age", "city")
                  .primaryKeys("name")
                  .primaryKeys("age")
                  .create();
        });
    }

    @Test
    public void testAddVertexLabelWithPrimaryKeyContainSameProp() {
        super.initPropertyKeys();
        SchemaManager schema = graph().schema();

        Assert.assertThrows(IllegalArgumentException.class, () -> {
            schema.vertexLabel("person")
                  .properties("name", "age", "city")
                  .primaryKeys("name", "age", "name")
                  .create();
        });
    }

    @Test
    public void testAddVertexLabelWithoutPrimaryKey() {
        super.initPropertyKeys();
        SchemaManager schema = graph().schema();

        Assert.assertThrows(IllegalArgumentException.class, () -> {
            schema.vertexLabel("person")
                  .usePrimaryKeyId()
                  .properties("name", "age", "city")
                  .create();
        });
    }

    @Test
    public void testAddVertexLabelWithoutPropertyKey() {
        super.initPropertyKeys();
        SchemaManager schema = graph().schema();

        VertexLabel person = schema.vertexLabel("person").create();

        Assert.assertTrue(person.properties().isEmpty());
    }

    @Test
    public void testAddVertexLabelWithPrimaryKeyNotInProperty() {
        super.initPropertyKeys();
        SchemaManager schema = graph().schema();

        Assert.assertThrows(IllegalArgumentException.class, () -> {
            schema.vertexLabel("person")
                  .properties("name", "age", "city")
                  .primaryKeys("name", "sex")
                  .ifNotExist()
                  .create();
        });

        Assert.assertThrows(IllegalArgumentException.class, () -> {
            schema.vertexLabel("person")
                  .primaryKeys("name", "sex")
                  .ifNotExist()
                  .create();
        });
    }

    @Test
    public void testAddVertexLabelWithNotExistProperty() {
        super.initPropertyKeys();
        SchemaManager schema = graph().schema();

        Assert.assertThrows(IllegalArgumentException.class, () -> {
            schema.vertexLabel("person").properties("sex").create();
        });
    }

    @Test
    public void testAddVertexLabelWithNullableKeys() {
        super.initPropertyKeys();
        SchemaManager schema = graph().schema();

        VertexLabel person = schema.vertexLabel("person")
                             .properties("name", "age", "city")
                             .primaryKeys("name")
                             .nullableKeys("city")
                             .create();

        Assert.assertNotNull(person);
        Assert.assertEquals("person", person.name());
        Assert.assertEquals(3, person.properties().size());
        assertContainsPk(person.properties(), "name", "age", "city");
        Assert.assertEquals(1, person.primaryKeys().size());
        assertContainsPk(person.primaryKeys(), "name");
        Assert.assertEquals(1, person.nullableKeys().size());
        assertContainsPk(person.nullableKeys(), "city");
    }

    @Test
    public void testAddVertexLabelWithUndefinedNullableKeys() {
        super.initPropertyKeys();
        SchemaManager schema = graph().schema();

        Assert.assertThrows(IllegalArgumentException.class, () -> {
            schema.vertexLabel("person")
                  .properties("name", "age", "city")
                  .primaryKeys("name")
                  .nullableKeys("undefined")
                  .create();
        });
    }

    @Test
    public void testAddVertexLabelWithNullableKeysNotInProperties() {
        super.initPropertyKeys();
        SchemaManager schema = graph().schema();

        Assert.assertThrows(IllegalArgumentException.class, () -> {
            schema.vertexLabel("person")
                  .properties("name", "age", "city")
                  .primaryKeys("name")
                  .nullableKeys("time")
                  .create();
        });
    }

    @Test
    public void testAddVertexLabelWithNullableKeysIntersectPrimarykeys() {
        super.initPropertyKeys();
        SchemaManager schema = graph().schema();

        Assert.assertThrows(IllegalArgumentException.class, () -> {
            schema.vertexLabel("person")
                  .properties("name", "age", "city")
                  .primaryKeys("name")
                  .nullableKeys("name")
                  .create();
        });

        Assert.assertThrows(IllegalArgumentException.class, () -> {
            schema.vertexLabel("person")
                  .properties("name", "age", "city")
                  .primaryKeys("name", "age")
                  .nullableKeys("age")
                  .create();
        });

        Assert.assertThrows(IllegalArgumentException.class, () -> {
            schema.vertexLabel("person")
                  .properties("name", "age", "city")
                  .primaryKeys("name")
                  .nullableKeys("name", "age")
                  .create();
        });
    }

    @Test
    public void testAppendVertexLabelWithUndefinedNullableKeys() {
        super.initPropertyKeys();
        SchemaManager schema = graph().schema();

        schema.vertexLabel("person")
              .properties("name", "age")
              .primaryKeys("name")
              .nullableKeys("age")
              .create();

        Assert.assertThrows(IllegalArgumentException.class, () -> {
            schema.vertexLabel("person")
                  .properties("city")
                  .nullableKeys("undefined")
                  .append();
        });
    }

    @Test
    public void testAppendVertexLabelWithNullableKeysInOriginProperties() {
        super.initPropertyKeys();
        SchemaManager schema = graph().schema();

        schema.vertexLabel("person")
              .properties("name", "age", "city")
              .primaryKeys("name")
              .nullableKeys("age")
              .create();

        VertexLabel person = schema.vertexLabel("person")
                             .nullableKeys("city")
                             .append();

        Assert.assertNotNull(person);
        Assert.assertEquals("person", person.name());
        Assert.assertEquals(3, person.properties().size());
        assertContainsPk(person.properties(), "name", "age", "city");
        Assert.assertEquals(1, person.primaryKeys().size());
        assertContainsPk(person.primaryKeys(), "name");
        Assert.assertEquals(2, person.nullableKeys().size());
        assertContainsPk(person.nullableKeys(), "age", "city");
    }

    @Test
    public void testAppendVertexLabelWithNullableKeysInAppendProperties() {
        super.initPropertyKeys();
        SchemaManager schema = graph().schema();

        schema.vertexLabel("person")
              .properties("name", "age")
              .primaryKeys("name")
              .create();

        VertexLabel person = schema.vertexLabel("person")
                             .properties("city")
                             .nullableKeys("city")
                             .append();

        Assert.assertNotNull(person);
        Assert.assertEquals("person", person.name());
        Assert.assertEquals(3, person.properties().size());
        assertContainsPk(person.properties(), "name", "age", "city");
        Assert.assertEquals(1, person.primaryKeys().size());
        assertContainsPk(person.primaryKeys(), "name");
        Assert.assertEquals(1, person.nullableKeys().size());
        assertContainsPk(person.nullableKeys(), "city");
    }

    @Test
    public void testAppendVertexLabelWithNullableKeysNotInProperties() {
        super.initPropertyKeys();
        SchemaManager schema = graph().schema();

        schema.vertexLabel("person")
              .properties("name", "age", "city")
              .primaryKeys("name")
              .create();
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            schema.vertexLabel("person")
                  .nullableKeys("time")
                  .append();
        });
    }

    @Test
    public void testAppendVertexLabelWithNonNullProperties() {
        super.initPropertyKeys();
        SchemaManager schema = graph().schema();

        schema.vertexLabel("person")
              .properties("name", "age")
              .primaryKeys("name")
              .create();
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            schema.vertexLabel("person")
                  .properties("city")
                  .append();
        });
    }

    @Test
    public void testRemoveVertexLabel() {
        super.initPropertyKeys();
        SchemaManager schema = graph().schema();

        schema.vertexLabel("person")
              .properties("name", "age", "city")
              .primaryKeys("name")
              .create();

        Assert.assertNotNull(schema.getVertexLabel("person"));

        schema.vertexLabel("person").remove();

        Assert.assertThrows(NotFoundException.class, () -> {
            schema.getVertexLabel("person");
        });
    }

    @Test
    public void testRemoveNotExistVertexLabel() {
        SchemaManager schema = graph().schema();
        schema.vertexLabel("not-exist-vl").remove();
    }

    @Test
    public void testRemoveVertexLabelWithVertex() {
        super.initPropertyKeys();
        SchemaManager schema = graph().schema();

        schema.vertexLabel("person")
              .properties("name", "age", "city")
              .primaryKeys("name")
              .nullableKeys("city")
              .create();

        graph().addVertex(T.label, "person", "name", "marko", "age", 22);
        graph().addVertex(T.label, "person", "name", "jerry", "age", 5);
        graph().addVertex(T.label, "person", "name", "tom", "age", 8);
        graph().tx().commit();

        List<Vertex> vertex = graph().traversal().V().hasLabel("person")
                              .toList();
        Assert.assertNotNull(vertex);
        Assert.assertEquals(3, vertex.size());

        schema.vertexLabel("person").remove();

        Assert.assertThrows(NotFoundException.class, () -> {
            schema.getVertexLabel("person");
        });

        Assert.assertThrows(IllegalArgumentException.class, () -> {
            graph().traversal().V().hasLabel("person").toList();
        });
    }

    @Test
    public void testRemoveVertexLabelWithVertexAndRangeIndex() {
        Assume.assumeTrue("Not support range condition query",
                          storeFeatures().supportsQueryWithRangeCondition());
        super.initPropertyKeys();
        SchemaManager schema = graph().schema();

        schema.vertexLabel("person")
              .properties("name", "age", "city")
              .primaryKeys("name")
              .nullableKeys("city")
              .create();

        schema.indexLabel("personByAge").onV("person").by("age").range()
              .create();

        graph().addVertex(T.label, "person", "name", "marko", "age", 22);
        graph().addVertex(T.label, "person", "name", "jerry", "age", 5);
        graph().addVertex(T.label, "person", "name", "tom", "age", 8);
        graph().tx().commit();

        List<Vertex> vertex = graph().traversal().V().hasLabel("person")
                              .has("age", P.inside(4, 10)).toList();
        Assert.assertNotNull(vertex);
        Assert.assertEquals(2, vertex.size());

        schema.vertexLabel("person").remove();

        Assert.assertThrows(NotFoundException.class, () -> {
            schema.getVertexLabel("person");
        });

        Assert.assertThrows(NotFoundException.class, () -> {
            schema.getIndexLabel("personByAge");
        });

        Assert.assertThrows(IllegalArgumentException.class, () -> {
            graph().traversal().V().hasLabel("person").toList();
        });
    }

    @Test
    public void testRemoveVertexLabelWithVertexAndSecondaryIndex() {
        super.initPropertyKeys();
        SchemaManager schema = graph().schema();

        schema.vertexLabel("person")
              .properties("name", "age", "city")
              .primaryKeys("name")
              .nullableKeys("age")
              .create();

        schema.indexLabel("personByCity").onV("person").by("city").secondary()
              .create();

        graph().addVertex(T.label, "person", "name", "marko",
                          "city", "Beijing");
        graph().addVertex(T.label, "person", "name", "jerry",
                          "city", "Beijing");
        graph().addVertex(T.label, "person", "name", "tom",
                          "city", "HongKong");
        graph().tx().commit();

        List<Vertex> vertex = graph().traversal().V().hasLabel("person")
                              .has("city", "Beijing").toList();
        Assert.assertNotNull(vertex);
        Assert.assertEquals(2, vertex.size());

        schema.vertexLabel("person").remove();

        Assert.assertThrows(NotFoundException.class, () -> {
            schema.getVertexLabel("person");
        });

        Assert.assertThrows(NotFoundException.class, () -> {
            schema.getIndexLabel("personByCity");
        });

        Assert.assertThrows(IllegalArgumentException.class, () -> {
            graph().traversal().V().hasLabel("person").toList();
        });
    }

    @Test
    public void testRemoveVertexLabelUsedByEdgeLabel() {
        super.initPropertyKeys();
        SchemaManager schema = graph().schema();

        schema.vertexLabel("person")
              .properties("name", "age", "city")
              .primaryKeys("name")
              .nullableKeys("city")
              .create();

        schema.vertexLabel("book")
              .properties("name")
              .primaryKeys("name")
              .create();

        schema.edgeLabel("write").link("person", "book")
              .properties("time", "weight")
              .create();

        Vertex marko = graph().addVertex(T.label, "person", "name", "marko",
                                         "age", 22);
        Vertex java = graph().addVertex(T.label, "book",
                                        "name", "java in action");

        marko.addEdge("write", java, "time", "2016-12-12", "weight", 0.3);

        Assert.assertThrows(HugeException.class, () -> {
            schema.vertexLabel("person").remove();
        });

        Assert.assertThrows(HugeException.class, () -> {
            schema.vertexLabel("book").remove();
        });
    }
}