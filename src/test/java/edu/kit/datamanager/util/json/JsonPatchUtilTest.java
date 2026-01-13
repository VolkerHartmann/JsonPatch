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
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class JsonPatchUtilTest {
  // simple POJO
  record Person(String name, int age) {}

  @Test
  public void applyPatchToJsonNodeWithJsonPatch() {
    JsonMapper mapper = JsonMapper.builder().build();
    JsonNode original = mapper.readTree("{\"name\":\"Alice\",\"age\":30}");

    List<JsonPatch.Operation> ops = List.of(
            new JsonPatch.Operation(JsonPatch.OperationType.REPLACE, "/name", null, "Bob"),
            new JsonPatch.Operation(JsonPatch.OperationType.ADD, "/city", null, "Karlsruhe")
    );
    JsonPatch patch = new JsonPatch(ops);

    JsonNode patched = JsonPatchUtil.applyPatch(original, patch);

    assertEquals("Bob", patched.get("name").asString());
    assertEquals(30, patched.get("age").asInt());
    assertEquals("Karlsruhe", patched.get("city").asString());
  }

  @Test
  public void applyPatchToJsonNodeWithJsonNode() {
    JsonMapper mapper = JsonMapper.builder().build();
    JsonNode original = mapper.readTree("{\"name\":\"Alice\",\"age\":30}");

    List<JsonPatch.Operation> ops = List.of(
            new JsonPatch.Operation(JsonPatch.OperationType.REPLACE, "/name", null, "Bob"),
            new JsonPatch.Operation(JsonPatch.OperationType.ADD, "/city", null, "Karlsruhe")
    );
    JsonNode patchNode = mapper.valueToTree(new JsonPatch(ops));

    JsonNode patched = JsonPatchUtil.applyPatch(original, patchNode);

    assertEquals("Bob", patched.get("name").asString());
    assertEquals(30, patched.get("age").asInt());
    assertEquals("Karlsruhe", patched.get("city").asString());
  }

  @Test
  public void applyPatchWithNonArrayPatchShouldThrow() {
    JsonMapper mapper = JsonMapper.builder().build();
    JsonNode original = mapper.readTree("{\"name\":\"Alice\"}");

    // patch as an object instead of array
    JsonNode invalidPatch = mapper.readTree("{\"op\":\"replace\"}");

    assertThrows(JsonPatchProcessingException.class, () -> JsonPatchUtil.applyPatch(original, invalidPatch));
  }

  @Test
  public void applyPatchPojo_blockedPathShouldThrow() {
    String name = "Betty";
    int age = 31;
    Person original = new Person(name, age);
    String patchJson = "[ { \"op\": \"replace\", \"path\": \"/name\", \"value\": \"Bob\" } ]";

    PatchOptions options = PatchOptions.ofBlockedPaths(Set.of("/name"));

    assertThrows(JsonPatchProcessingException.class, () -> JsonPatchUtil.applyPatch(original, patchJson, Person.class, options));
    assertEquals(name, original.name);
    assertEquals(age, original.age);
  }

  @Test
  public void applyPatchPojo() {
    String name = "Caroline";
    int age = 32;
    Person original = new Person(name, age);

    String patchJson = "[ { \"op\": \"replace\", \"path\": \"/name\", \"value\": \"Bob\" } ]";

    Person updatedPerson = JsonPatchUtil.applyPatch(original, patchJson, Person.class);
    assertEquals("Bob", updatedPerson.name);
    assertEquals(32, updatedPerson.age);
  }

  @Test
  public void applyPatchPojo_withUnknownField() {
    Person original = new Person("Doreen", 33);

    String patchJson = "[ { \"op\": \"replace\", \"path\": \"/address\", \"value\": \"new address\" } ]";

    assertThrows(JsonPatchProcessingException.class, () -> JsonPatchUtil.applyPatch(original, patchJson, Person.class));
  }

  @Test
  public void applyMergePath_happyPatchAndInvalidType() {
    String original = "{\"a\":1,\"b\":2}";
    String mergePatch = "{\"b\":3}";

    String result = JsonPatchUtil.applyMergePatch(original, mergePatch);
    assertTrue(result.contains("\"b\":3"));

    // invalid merge patch (array instead of object) should throw
    String invalidMerge = "[1,2,3]";
    assertThrows(JsonPatchProcessingException.class, () -> JsonPatchUtil.applyMergePatch(original, invalidMerge));
  }

  @Test
  public void applyPatch_string_string() {
    String original = "{\"a\":1,\"b\":2}";
    String patch = "[{ \"op\": \"replace\", \"path\": \"/b\", \"value\": 3 }]";

    String result = JsonPatchUtil.applyPatch(original, patch);
    assertTrue(result.contains("\"b\":3"));
  }

  @Test
  public void applyPatch_string_jsonPatch() {
    String original = "{\"a\":1,\"b\":2}";
    String patch = "[{ \"op\": \"replace\", \"path\": \"/b\", \"value\": 3 }]";

    String result = JsonPatchUtil.applyPatch(original, JsonPatchUtil.jsonStringToObject(patch, JsonPatch.class));
    assertTrue(result.contains("\"b\":3"));
  }

  @Test
  public void applyPatch_string_jsonNode() {
    String original = "{\"a\":1,\"b\":2}";
    String patch = "[{ \"op\": \"replace\", \"path\": \"/b\", \"value\": 3 }]";

    String result = JsonPatchUtil.applyPatch(original, JsonPatchUtil.jsonStringToNode(patch));
    assertTrue(result.contains("\"b\":3"));
  }

  @Test
  public void applyPatch_jsonNode_string() {
    String original = "{\"a\":1,\"b\":2}";
    String patch = "[{ \"op\": \"replace\", \"path\": \"/b\", \"value\": 3 }]";

    JsonNode jsonNode = JsonPatchUtil.applyPatch(JsonPatchUtil.jsonStringToNode(original), patch);
    assertEquals(3, jsonNode.get("b").intValue());
  }
  @Test
  public void applyPatch_jsonNode_jsonPatch() {
    String original = "{\"a\":1,\"b\":2}";
    String patch = "[{ \"op\": \"replace\", \"path\": \"/b\", \"value\": 3 }]";

    JsonNode jsonNode = JsonPatchUtil.applyPatch(JsonPatchUtil.jsonStringToNode(original), JsonPatchUtil.jsonStringToObject(patch, JsonPatch.class));
    assertEquals(3, jsonNode.get("b").intValue());
  }


}

