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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.List;

/**
 * JSON structure holding an array of patches.
 *
 * @param operations List of operations in this patch.
 */
public record JsonPatch(List<Operation> operations) {
  /**
   * Enumeration of supported JSON Patch operations.
   */
  public enum OperationType {
    ADD("add"), REMOVE("remove"), REPLACE("replace"), MOVE("move"), COPY("copy"), TEST("test");
    /* String representation for JSON. */
    private final String op;

    OperationType(String op) {
      this.op = op;
    }

    /**
     * Use op for (JSON) serialization.
     *
     * @return String representation of the operation
     */
    @JsonValue
    public String jsonValue() {
      return op;
    }

    /**
     * Ignore case for deserializing JSON value.
     *
     * @param value JSON value
     * @return Corresponding OperationType
     */
    @JsonCreator
    public static OperationType fromJson(String value) {
      if (value == null) return null;
      for (OperationType t : values()) {
        System.out.println("Comparing " + t.op + " and " + t.name() + " and " + value);
        if (t.op.equalsIgnoreCase(value)) {
          return t;
        }
      }
      throw new IllegalArgumentException("Unknown operation type: " + value);
    }

    @Override
    public String toString() {
      return op;
    }
  }

  /**
   * Constructor for JSON deserialization.
   *
   * @param operations List of operations in this patch.
   */
  @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
  public JsonPatch {
  }

  /**
   * Get the list of operations in this patch.
   *
   * @return List of operations.
   */
  @Override
  @JsonValue
  public List<Operation> operations() {
    return operations;
  }

  /**
   * Representation of a single JSON Patch operation as a nested record.
   * Accessible from outside as `JsonPatch.Operation`.
   */
  public record Operation(OperationType op, String path, String from, Object value) {
  }
}