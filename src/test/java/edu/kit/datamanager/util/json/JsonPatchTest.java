/*
 * Copyright 2025 Karlsruhe Institute of Technology.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.kit.datamanager.util.json;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class JsonPatchTest {

  @Test
  public void defaultConstructorShouldHaveNullOperations() {
    JsonPatch p = new JsonPatch(null);
    assertNull(p.operations(), "operations should be null by default");
  }

  @Test
  public void parameterizedConstructorAndGetters() {
    List<JsonPatch.Operation> ops = new ArrayList<>();
    ops.add(new JsonPatch.Operation(JsonPatch.OperationType.ADD, "/a", null, "value"));

    JsonPatch p = new JsonPatch(ops);
    assertSame(ops, p.operations());
    assertEquals(1, p.operations().size());
  }

  @Test
  public void parameterizedConstructorAndGetters2() {
    JsonPatch.Operation op = new JsonPatch.Operation(JsonPatch.OperationType.ADD, "/a", null, "value");
    List<JsonPatch.Operation> ops = new ArrayList<>();
    ops.add(op);
    JsonPatch p = new JsonPatch(ops);
    assertSame(ops.getFirst(), p.operations().getFirst());
    assertEquals(1, p.operations().size());
  }

  @Test
  public void settersShouldUpdateOperationsAndAllowNulls() {
    JsonPatch p;
    List<JsonPatch.Operation> ops = new ArrayList<>();
    ops.add(new JsonPatch.Operation(JsonPatch.OperationType.REMOVE, "/b", null, null));

    p = new JsonPatch(ops);
    assertSame(ops, p.operations());
  }

  @Test
  public void operationsListCanBeEmpty() {
    JsonPatch p = new JsonPatch(new ArrayList<>());
    assertNotNull(p.operations());
    assertTrue(p.operations().isEmpty());
  }

  @Test
  public void testSerializationAndDeserialization() {
    String json = """
      [{"op":"replace","path":"/name","from":null,"value":"Bob"},
       {"op":"remove","path":"/address","from":null,"value":null},
       {"op":"add","path":"/age","from":null,"value":"30"}]
   """;
    JsonMapper mapper = JsonMapper.builder().build();
    JsonPatch patch = mapper.readValue(json, JsonPatch.class);
    assertNotNull(patch.operations());
    assertEquals(3, patch.operations().size());
    String jsonPatch = mapper.writeValueAsString(patch);
    assertNotNull(jsonPatch);
    assertEquals( json.replaceAll("\\s",""),jsonPatch);
  }
  @Test
  public void testAddingAnObject() {
    String json = """
      [{"op":"add","path":"/person","from":null,"value":{"name":"Alice","age":25}}]
   """;
    JsonMapper mapper = JsonMapper.builder().build();
    JsonPatch patch = mapper.readValue(json, JsonPatch.class);
    assertNotNull(patch.operations());
    assertEquals(1, patch.operations().size());
    JsonPatch.Operation op = patch.operations().get(0);
    assertEquals(JsonPatch.OperationType.ADD, op.op());
    assertEquals("/person", op.path());
    assertNull(op.from());
    assertNotNull(op.value());
    assertInstanceOf(Map.class, op.value());
    @SuppressWarnings("unchecked")
    java.util.Map<String,Object> valueMap = (java.util.Map<String,Object>)op.value();
    assertEquals("Alice", valueMap.get("name"));
    assertEquals(25, valueMap.get("age"));
  }
}
