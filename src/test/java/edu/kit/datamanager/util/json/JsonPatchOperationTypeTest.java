package edu.kit.datamanager.util.json;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JsonPatchOperationTypeTest {
  @Test
  public void testToString() {
    assertEquals("add", JsonPatch.OperationType.ADD.toString());
    assertEquals("remove", JsonPatch.OperationType.REMOVE.toString());
    assertEquals("replace", JsonPatch.OperationType.REPLACE.toString());
    assertEquals("move", JsonPatch.OperationType.MOVE.toString());
    assertEquals("copy", JsonPatch.OperationType.COPY.toString());
    assertEquals("test", JsonPatch.OperationType.TEST.toString());
  }
  @Test
  public void testToJson() {
    assertEquals("add", JsonPatch.OperationType.ADD.jsonValue());
    assertEquals("remove", JsonPatch.OperationType.REMOVE.jsonValue());
    assertEquals("replace", JsonPatch.OperationType.REPLACE.jsonValue());
    assertEquals("move", JsonPatch.OperationType.MOVE.jsonValue());
    assertEquals("copy", JsonPatch.OperationType.COPY.jsonValue());
    assertEquals("test", JsonPatch.OperationType.TEST.jsonValue());
  }
  @Test
  public void testFromJson() {
    assertEquals(JsonPatch.OperationType.ADD, JsonPatch.OperationType.fromJson("add"));
    assertEquals(JsonPatch.OperationType.REMOVE, JsonPatch.OperationType.fromJson("remove"));
    assertEquals(JsonPatch.OperationType.REPLACE, JsonPatch.OperationType.fromJson("replace"));
    assertEquals(JsonPatch.OperationType.MOVE, JsonPatch.OperationType.fromJson("move"));
    assertEquals(JsonPatch.OperationType.COPY, JsonPatch.OperationType.fromJson("copy"));
    assertEquals(JsonPatch.OperationType.TEST, JsonPatch.OperationType.fromJson("test"));
  }
  @Test
  public void testFromJsonWithInvalidValue() {
    Exception exception = assertThrows(IllegalArgumentException.class, () -> {
      JsonPatch.OperationType.fromJson("invalid");
    });
    assertEquals("Unknown operation type: invalid", exception.getMessage());
  }
  @Test
  public void testFromJsonWithNullValue() {
    JsonPatch.OperationType operationType = JsonPatch.OperationType.fromJson(null);
    assertNull(operationType);
  }
}