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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class JsonPatchIntegrationTest {

  // Static nested test POJOs so Jackson can construct/deserialize them
  public static class InnerClass { public String name; public int[] numbers; }
  public static class Container { public InnerClass inner; }

  @Test
  public void deepNestedReplaceAddMoveCopy() {
    JsonMapper mapper = JsonMapper.builder().build();
    String originalJson = """
        {
          "root": {
            "level1": {
              "level2": {
                "value": "old",
                "arr": [ { "id": 1, "items": ["a","b"] }, { "id": 2 } ]
              }
            },
            "other": { "copyMe": { "f": true } }
          }
        }""";

    JsonNode original = mapper.readTree(originalJson);

    List<JsonPatch.Operation> ops = List.of(
      // replace nested value
      new JsonPatch.Operation(JsonPatch.OperationType.REPLACE, "/root/level1/level2/value", null, "new"),
      // add to nested array inside object index 0
      new JsonPatch.Operation(JsonPatch.OperationType.ADD, "/root/level1/level2/arr/0/items/1", null, "x"),
      // move id from first element to top-level under other.movedId
      new JsonPatch.Operation(JsonPatch.OperationType.MOVE, "/root/other/movedId", "/root/level1/level2/arr/0/id", null),
      // copy the object copyMe
      new JsonPatch.Operation(JsonPatch.OperationType.COPY, "/root/level1/copied", "/root/other/copyMe", null),
      // remove the second element from arr
      new JsonPatch.Operation(JsonPatch.OperationType.REMOVE, "/root/level1/level2/arr/1", null, null)
    );

    JsonPatch patch = new JsonPatch(ops);
    JsonNode patched = JsonPatchUtil.applyPatch(original, patch);

    // assertions
    assertEquals("new", patched.at("/root/level1/level2/value").asString());
    // after adding at index 1, items should be ["a","x","b"]
    JsonNode items = patched.at("/root/level1/level2/arr/0/items");
    assertEquals(3, items.size());
    assertEquals("x", items.get(1).asString());

    // moved id should exist
    assertEquals(1, patched.at("/root/other/movedId").asInt());
    // copied object should exist under /root/level1/copied/f
    assertTrue(patched.at("/root/level1/copied/f").asBoolean());

    // arr length should be 1 after removing index 1
    assertEquals(1, patched.at("/root/level1/level2/arr").size());
  }

  @Test
  public void applyPatchToPojoWithNestedObjects() {
    Container original = new Container();
    original.inner = new InnerClass();
    original.inner.name = "foo";
    original.inner.numbers = new int[]{1,2};

    String patchJson = """
      [
        { "op": "replace", "path": "/inner/name", "value": "bar" },
        { "op": "add", "path": "/inner/numbers/-", "value": 3 }
      ]""";

    Container patched = JsonPatchUtil.applyPatch(original, patchJson, Container.class);

    assertEquals("bar", patched.inner.name);
    assertArrayEquals(new int[]{1,2,3}, patched.inner.numbers);
  }

  @Test
  public void complexSequenceWithArraysAndObjects() {
    JsonMapper mapper = JsonMapper.builder().build();
    String originalJson = "{\"list\":[{\"k\":1},{\"k\":2},{\"k\":3}],\"map\":{\"a\":{\"v\":10}}}";
    JsonNode original = mapper.readTree(originalJson);

    List<JsonPatch.Operation> ops = List.of(
      // remove middle element
      new JsonPatch.Operation(JsonPatch.OperationType.REMOVE, "/list/1", null, null),
      // add new element at index 1
      new JsonPatch.Operation(JsonPatch.OperationType.ADD, "/list/1", null, mapper.readTree("{\"k\":20}")),
      // replace nested value
      new JsonPatch.Operation(JsonPatch.OperationType.REPLACE, "/map/a/v", null, 99),
      // copy map.a to map.b
      new JsonPatch.Operation(JsonPatch.OperationType.COPY, "/map/b", "/map/a", null),
      // move list/2 (old index 2 -> after operations may differ) to /moved
      new JsonPatch.Operation(JsonPatch.OperationType.MOVE, "/moved", "/list/2", null)
    );

    JsonPatch patch = new JsonPatch(ops);
    JsonNode patched = JsonPatchUtil.applyPatch(original, patch);

    // list should have 2 elements after remove+add (indexes 0 and 1)
    assertEquals(2, patched.at("/list").size());
    assertEquals(20, patched.at("/list/1/k").asInt());

    // map.a.v updated
    assertEquals(99, patched.at("/map/a/v").asInt());
    // map.b exists and equals map.a
    assertEquals(99, patched.at("/map/b/v").asInt());

    // moved node exists at /moved
    assertTrue(patched.has("moved"));
  }
}
