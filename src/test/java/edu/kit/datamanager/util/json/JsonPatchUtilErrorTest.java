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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class JsonPatchUtilErrorTest {

  record Customer(String id, java.util.List<String> favorites, String telephone) {}


  @BeforeAll
  static void enableReset() {
    System.setProperty("json.patch.resetForTesting", "true");
  }

  @AfterEach
  void reset() {
    JsonPatchUtil.resetForTesting();
  }

  @Test
  void testConstructorWithDefaultMapper() {
    JsonMapper defaultMapper = JsonPatchUtil.getDefaultMapper();
    assertFalse(defaultMapper.isEnabled(SerializationFeature.INDENT_OUTPUT));
  }

  @Test
  void testConstructorWithInitializedMapper() {
    JsonPatchUtil.configureMapper(builder -> builder.configure(SerializationFeature.INDENT_OUTPUT, true));
    JsonMapper defaultMapper = JsonPatchUtil.getDefaultMapper();
    assertTrue(defaultMapper.isEnabled(SerializationFeature.INDENT_OUTPUT));
  }

  @Test
  void testConstructorWithInitializeMapperAfterwards_throwsJsonProcessingException() {
    JsonMapper defaultMapper = JsonPatchUtil.getDefaultMapper();
    assertThrows(IllegalStateException.class, () ->JsonPatchUtil.configureMapper(builder -> builder.configure(SerializationFeature.INDENT_OUTPUT, true)));
    assertFalse(defaultMapper.isEnabled(SerializationFeature.INDENT_OUTPUT));
  }

  @Test
  void testResetForTestingWhileNotAllowed_throwsIllegalStateException() {
    System.setProperty("json.patch.resetForTesting", "false");
    JsonMapper defaultMapper = JsonPatchUtil.getDefaultMapper();
    assertNotNull(defaultMapper);
    assertThrows(IllegalStateException.class, JsonPatchUtil::resetForTesting);
    System.setProperty("json.patch.resetForTesting", "true");
  }

  @Test
  void testApplyJsonPatch_malformedJson_throwsPatchProcessingException() {
    var original = new Customer("1", java.util.List.of("Milk"), "001-111-1234");
    String badPatch = "not-a-json";

    assertThrows(JsonPatchProcessingException.class,
            () -> JsonPatchUtil.applyPatch(original, badPatch, Customer.class));
  }

  @Test
  void testApplyJsonPatch_patchNotArray_throwsPatchProcessingException() {
     var original = new Customer("2", java.util.List.of("Milk"), "001-222-1234");
    // Object instead of array => RFC 6902 requires an array of operations
    String patchObject = "{\"op\":\"replace\",\"path\":\"/telephone\",\"value\":\"000\"}";

    assertThrows(JsonPatchProcessingException.class,
            () -> JsonPatchUtil.applyPatch(original, patchObject, Customer.class));
  }

  @Test
  void testApplyJsonPatch_addNonExistingField_throwsPatchProcessingException() {
    var original = new Customer("3", java.util.List.of("Milk", "Eggs"), "001-333-1234");
    // Try to add a non-existing field
    String patch = "[{ \"op\": \"add\", \"path\": \"/unknownField\", \"value\": \"SomeValue\" }]";
    // Should work with strings
    String patchedJson = JsonPatchUtil.applyPatch(JsonPatchUtil.jsonObjectToString(original), patch);
    assertTrue(patchedJson.contains("\"unknownField\":\"SomeValue\""));
    assertThrows(JsonPatchProcessingException.class,
            () -> JsonPatchUtil.applyPatch(original, patch, Customer.class));
    String validPatch = "[{ \"op\": \"add\", \"path\": \"/telephone\", \"value\": \"001-333-6789\" }]";
    Customer patchedCustomer = JsonPatchUtil.applyPatch(original, validPatch, Customer.class);
    assertEquals("001-333-6789", patchedCustomer.telephone());
  }

  @Test
  void testApplyJsonPatch_removeNonExistingPath_throwsPatchProcessingException() {
    var original = new Customer("4", java.util.List.of("Milk", "Eggs"), "001-444-1234");
    // Try to remove non-existing index 10
    String patch = "[{ \"op\": \"remove\", \"path\": \"/favorites/10\" }]";

    assertThrows(JsonPatchProcessingException.class,
            () -> JsonPatchUtil.applyPatch(original, patch, Customer.class));
  }

  @Test
  void testApplyJsonPatch_fromBlockedPath_throwsPatchProcessingException() {
    var original = new Customer("5", java.util.List.of("Milk"), "001-555-1234");
    // Try to move between paths, where "from" is blocked
    String patch = "[{ \"op\": \"move\", \"from\": \"/id\", \"path\": \"/telephone\" }]";
    var options = PatchOptions.ofBlockedPaths(Set.of("/id"));

    assertThrows(JsonPatchProcessingException.class,
            () -> JsonPatchUtil.applyPatch(original, patch, Customer.class, options));
  }

  @Test
  void testApplyJsonMergePatch_notObject_throwsPathProcessingException() {
    var original = new Customer("6", java.util.List.of("Milk"), "001-666-1234");
    // Invalid merge patch
    String mergePatchArray = "[ \"telephone\", \"000\" ]";

    assertThrows(JsonPatchProcessingException.class,
            () -> JsonPatchUtil.applyMergePatch(original, mergePatchArray, Customer.class));
  }

  @Test
  void testApplyPatch_addToNullArray_createsList() {
    var original = new Customer("7", null, "001-777-1234");
    String patch = "[{ \"op\": \"add\", \"path\": \"/favorites/-\", \"value\": \"Milk\" }]";
    // Can't add to null array
    assertThrows(JsonPatchProcessingException.class,
            () -> JsonPatchUtil.applyPatch(original, patch, Customer.class));
  }

  @Test
  void testApplyPatch_addToNullArray_createsList1() {
    var original = new Customer("8", null, "001-888-1234");
    String patch = "[{ \"op\": \"add\", \"path\": \"/favorites/0\", \"value\": \"Milk\" }]";
    // Can't add to null array
    assertThrows(JsonPatchProcessingException.class,
            () -> JsonPatchUtil.applyPatch(original, patch, Customer.class));
  }

  @Test
  void testApplyMergePatch_setFieldToNull_resultsNull() {
    var original = new Customer("9", java.util.List.of("Milk"), "001-999-1234");
    String mergePatch = "{\"telephone\":null}";

    var result = JsonPatchUtil.applyMergePatch(original, mergePatch, Customer.class);

    assertNull(result.telephone());
  }

  @Test
  void testApplyPatch_optionsWithEmptyBlockedPaths_allowsChange() {
    record CustomerMinimal(String id, String telephone) {}

    var original = new CustomerMinimal("1", "001-111-1234");
    String patch = "[{\"op\":\"replace\",\"path\":\"/id\",\"value\":\"2\"}]";
    var options = PatchOptions.ofBlockedPaths(java.util.Set.of()); // empty set

    var result = JsonPatchUtil.applyPatch(original, patch, CustomerMinimal.class, options);

    assertEquals("2", result.id());

    var original2 = new CustomerMinimal("3", "001-222-1234");
    var result2 = JsonPatchUtil.applyPatch(original2, patch, CustomerMinimal.class, options);
    assertEquals("2", result2.id());
  }

  @Test
  void testGetJsonObjectToString_withInvalidObject_throwsJsonProcessingException() {
    class EmptyClass {}
    JsonPatchUtil.configureMapper(builder -> builder.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, true));
    assertThrows(JsonPatchProcessingException.class,
            () -> JsonPatchUtil.jsonObjectToString(new EmptyClass()));
  }
  @Test
  void testGetJsonStringToObject_withValidObject_throwsJsonProcessingException() {
    String invalidJson = "{ invalid-json }";

    assertThrows(JsonPatchProcessingException.class,
            () -> JsonPatchUtil.jsonStringToObject(invalidJson, Customer.class));
  }

  @Test
  void testGetJsonStringToNode_withInvalidObject_throwsJsonProcessingException() {
    String invalidJson = "{ invalid-json }";

    assertThrows(JsonPatchProcessingException.class,
            () -> JsonPatchUtil.jsonStringToNode(invalidJson));
  }
}
