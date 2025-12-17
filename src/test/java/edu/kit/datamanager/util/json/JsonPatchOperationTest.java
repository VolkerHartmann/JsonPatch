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

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class JsonPatchOperationTest {

  @Test
  public void defaultConstructionViaRecordShouldWork() {
    JsonPatch.Operation op = new JsonPatch.Operation(null, null, null, null);
    assertNull(op.op(), "op should be null");
    assertNull(op.path(), "path should be null");
    assertNull(op.from(), "from should be null");
    assertNull(op.value(), "value should be null");
  }

  @Test
  public void parameterizedConstructionAndAccessors() {
    JsonPatch.OperationType opType = JsonPatch.OperationType.ADD;
    String path = "/a/b";
    String from = "/from/path";
    Map<String, Object> value = new HashMap<>();
    value.put("key", "val");

    JsonPatch.Operation op = new JsonPatch.Operation(opType, path, from, value);
    assertEquals(opType, op.op());
    assertEquals(path, op.path());
    assertEquals(from, op.from());
    assertSame(value, op.value());
  }

  @Test
  public void recordsAreImmutableAndAllowNulls() {
    JsonPatch.Operation op = new JsonPatch.Operation(JsonPatch.OperationType.REPLACE, "/x", "/y", 123);

    // record fields are final; verify values
    assertEquals(JsonPatch.OperationType.REPLACE, op.op());
    assertEquals("/x", op.path());
    assertEquals("/y", op.from());
    assertEquals(123, op.value());

    // create another record with nulls to represent edge inputs
    JsonPatch.Operation op2 = new JsonPatch.Operation(null, null, null, null);
    assertNull(op2.op());
    assertNull(op2.path());
    assertNull(op2.from());
    assertNull(op2.value());
  }

  @Test
  public void allowEmptyStringsAndEmptyObjects() {
    JsonPatch.Operation op = new JsonPatch.Operation(JsonPatch.OperationType.TEST, "", "", new HashMap<>());
    assertEquals(JsonPatch.OperationType.TEST, op.op());
    assertEquals("", op.path());
    assertEquals("", op.from());
    assertNotNull(op.value());
    assertTrue(((Map<?,?>)op.value()).isEmpty());
  }
}
