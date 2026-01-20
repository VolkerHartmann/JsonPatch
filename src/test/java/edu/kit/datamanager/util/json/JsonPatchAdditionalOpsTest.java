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

import edu.kit.datamanager.util.json.exceptions.JsonPatchProcessingException;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class JsonPatchAdditionalOpsTest {

  @Test
  public void moveOperationMovesValue() {
    JsonMapper mapper = JsonMapper.builder().build();
    JsonNode original = mapper.readTree("{\"a\":{\"value\":\"v\"},\"b\":{}}");

    List<JsonPatch.Operation> ops = List.of(new JsonPatch.Operation(JsonPatch.OperationType.MOVE, "/b/value", "/a/value", null));
    JsonPatch patch = new JsonPatch(ops);

    JsonNode patched = JsonPatchUtil.applyPatch(original, patch);

    assertEquals("v", patched.at("/b/value").asString());
    assertTrue(patched.at("/a/value").isMissingNode());
  }

  @Test
  public void copyOperationCopiesValue() {
    JsonMapper mapper = JsonMapper.builder().build();
    JsonNode original = mapper.readTree("{\"a\":{\"value\":\"v\"},\"b\":{}}");

    List<JsonPatch.Operation> ops = List.of(new JsonPatch.Operation(JsonPatch.OperationType.COPY, "/b/value", "/a/value", null));
    JsonPatch patch = new JsonPatch(ops);

    JsonNode patched = JsonPatchUtil.applyPatch(original, patch);

    assertEquals("v", patched.at("/b/value").asString());
    assertEquals("v", patched.at("/a/value").asString()); // original remains
  }

  @Test
  public void testOperationSucceedsAndFails() {
    JsonMapper mapper = JsonMapper.builder().build();
    JsonNode original = mapper.readTree("{\"x\":1}");

    // success
    List<JsonPatch.Operation> okOps = List.of(new JsonPatch.Operation(JsonPatch.OperationType.TEST, "/x", null, 1));
    JsonPatch okPatch = new JsonPatch(okOps);
    JsonNode res = JsonPatchUtil.applyPatch(original, okPatch);
    assertEquals(1, res.get("x").asInt());

    // fail
    List<JsonPatch.Operation> failOps = List.of(new JsonPatch.Operation(JsonPatch.OperationType.TEST, "/x", null, 2));
    JsonPatch failPatch = new JsonPatch(failOps);
    assertThrows(JsonPatchProcessingException.class, () -> JsonPatchUtil.applyPatch(original, failPatch));
  }

  @Test
  public void addToArrayWithDashAppends() {
    JsonMapper mapper = JsonMapper.builder().build();
    JsonNode original = mapper.readTree("{\"arr\":[1,2]}\n");

    List<JsonPatch.Operation> ops = List.of(new JsonPatch.Operation(JsonPatch.OperationType.ADD, "/arr/-", null, 3));
    JsonPatch patch = new JsonPatch(ops);

    JsonNode patched = JsonPatchUtil.applyPatch(original, patch);
    assertEquals(3, patched.at("/arr/2").asInt());
    assertEquals(3, patched.get("arr").size());
  }

  @Test
  public void removeArrayIndexRemovesElement() {
    JsonMapper mapper = JsonMapper.builder().build();
    JsonNode original = mapper.readTree("{\"arr\":[1,2,3]}\n");

    List<JsonPatch.Operation> ops = List.of(new JsonPatch.Operation(JsonPatch.OperationType.REMOVE, "/arr/1", null, null));
    JsonPatch patch = new JsonPatch(ops);

    JsonNode patched = JsonPatchUtil.applyPatch(original, patch);
    assertEquals(2, patched.get("arr").size());
    assertEquals(1, patched.at("/arr/0").asInt());
    assertEquals(3, patched.at("/arr/1").asInt());
  }

  @Test
  public void replaceNestedValue() {
    JsonMapper mapper = JsonMapper.builder().build();
    JsonNode original = mapper.readTree("{\"obj\":{\"nested\":\"old\"}}\n");

    List<JsonPatch.Operation> ops = List.of(new JsonPatch.Operation(JsonPatch.OperationType.REPLACE, "/obj/nested", null, "new"));
    JsonPatch patch = new JsonPatch(ops);

    JsonNode patched = JsonPatchUtil.applyPatch(original, patch);
    assertEquals("new", patched.at("/obj/nested").asString());
  }
}

