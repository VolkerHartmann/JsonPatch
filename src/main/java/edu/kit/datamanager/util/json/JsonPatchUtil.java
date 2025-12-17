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
import jakarta.json.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Utility, that applies JSON Patch (RFC 6902) and JSON Merge Patch (RFC 7396)
 * using Jakarta JSON-P and maps to POJOs via Jackson 3.
 */
public final class JsonPatchUtil {

  private static final Logger logger = LoggerFactory.getLogger(JsonPatchUtil.class);
  /** List holding mapper configurations. */
  private static final List<Consumer<JsonMapper.Builder>> DEFAULT_MAPPER_CONFIGURATIONS = new ArrayList<>();
  /** Flag indicating that mapper configuration is frozen. */
  private static volatile boolean frozen = false;
  /** Holder-Idiom for default mapper. */
  private static final AtomicReference<JsonMapper> DEFAULT_MAPPER = new AtomicReference<>();
  /**
   * Get the default ObjectMapper used by static methods.
   * @return Default JsonMapper
   */
  public static JsonMapper getDefaultMapper() {
    JsonMapper mapper = DEFAULT_MAPPER.get();
    if (mapper != null) {
      return mapper;
    }
    synchronized (DEFAULT_MAPPER_CONFIGURATIONS) {
      mapper = DEFAULT_MAPPER.get();
      if (mapper == null) {
        mapper = build();
        DEFAULT_MAPPER.set(mapper);
      }
    }
    return mapper;
  }
  /** Configure the default ObjectMapper used by static methods.
   * This method must be called before any static method is used,
   * otherwise an IllegalStateException is thrown.
   *
   * @param mapperConfiguration Consumer accepting a JsonMapper.Builder
   */
  public static void configureMapper(Consumer<JsonMapper.Builder> mapperConfiguration) {
    Objects.requireNonNull(mapperConfiguration, "mapperConfiguration cannot be null");
    if (frozen) {
      throw new IllegalStateException("Mapper configuration is frozen and can't be changed anymore!");
    }
    synchronized (DEFAULT_MAPPER_CONFIGURATIONS) {
      // double check to avoid race condition
      if (frozen) {
        throw new IllegalStateException("Mapper configuration is frozen and can't be changed anymore!");
      }
      DEFAULT_MAPPER_CONFIGURATIONS.add(mapperConfiguration);
    }
  }

  private static JsonMapper build() {
    frozen = true;
    JsonMapper.Builder builder = JsonMapper.builder();
    synchronized (DEFAULT_MAPPER_CONFIGURATIONS) {
      for (Consumer<JsonMapper.Builder> config : DEFAULT_MAPPER_CONFIGURATIONS) {
        logger.trace("Configuring JsonMapper using config {}", config);
        config.accept(builder);
      }
      builder.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
      DEFAULT_MAPPER_CONFIGURATIONS.clear();
    }
    return builder.build();
  }
  /** Private constructor for utility class. */
  private JsonPatchUtil() {}

  /** Applies a JSON Patch (RFC 6902) to a JSON string.
   * (If you are still using Jackson 2, use this method.)
   * @param originalJson Original JSON as String
   * @param patchJson JSON Patch as String (Array of operations)
   * @return Patched JSON as String
   * @throws JsonPatchProcessingException in case of processing errors
   */
  public static String applyPatch(String originalJson, String patchJson) throws JsonPatchProcessingException {
    return applyPatch(originalJson, patchJson, (PatchOptions) null);
  }
  /** Applies a JSON Patch (RFC 6902) to a JSON string.
   * (If you are still using Jackson 2, use this method.)
   * @param originalJson Original JSON as String
   * @param jsonPatch JSON Patch as String (Array of operations)
   * @return Patched JSON as String
   * @throws JsonPatchProcessingException in case of processing errors
   */
  public static String applyPatch(String originalJson, JsonPatch jsonPatch) throws  JsonPatchProcessingException {
    return applyPatch(originalJson, jsonPatch, (PatchOptions) null);
  }
  /** Applies a JSON Patch (RFC 6902) to a JSON string.
   * @param originalJson Original JSON as String
   * @param patchJson JSON Patch as JsonNode (Array of operations)
   * @return Patched JSON as String
   * @throws JsonPatchProcessingException in case of processing errors
   */
  public static String applyPatch(String originalJson, JsonNode patchJson) throws  JsonPatchProcessingException {
    return applyPatch(originalJson, patchJson, (PatchOptions) null);
  }
  /** Applies a JSON Patch (RFC 6902) to a JsonNode.
   *
   * @param original Original JsonNode
   * @param jsonPatch JSON Patch as String (Array of operations)
  * @return Patched JsonNode
   * @throws JsonPatchProcessingException in case of processing errors
   */
  public static JsonNode applyPatch(JsonNode original, String jsonPatch) throws  JsonPatchProcessingException {
    return applyPatch(original, jsonPatch, (PatchOptions) null);
  }
  /**
   * Applies a JSON Patch (RFC 6902) to a JsonNode.
   *
   * @param original Original JsonNode
   * @param jsonPatch JSON Patch as JsonPatch object
   * @return Patched JsonNode
   * @throws JsonPatchProcessingException in case of processing errors
   */
  public static JsonNode applyPatch(JsonNode original, JsonPatch jsonPatch) throws   JsonPatchProcessingException {
    return applyPatch(original, jsonPatch, (PatchOptions) null);
  }
  /**
   * Applies a JSON Patch (RFC 6902) to a JsonNode.
   *
   * @param original Original JsonNode
   * @param patch JSON Patch as JsonNode (Array of operations)
   * @return Patched JsonNode
   * @throws JsonPatchProcessingException in case of processing errors
   */
  public static JsonNode applyPatch(JsonNode original, JsonNode patch) throws   JsonPatchProcessingException {
    return applyPatch(original, patch, (PatchOptions) null);
  }
  /**
   * Applies a JSON Patch (RFC 6902) to an object.
   *
   * @param original Original object
   * @param patchJson JSON Patch as String (Array of operations)
   * @param type target type
   * @return Patched object
   * @throws JsonPatchProcessingException in case of processing errors
   */
  public static <T> T  applyPatch(T original, String patchJson, Class<T> type) throws JsonPatchProcessingException {
    return applyPatch(original, patchJson, type, null);
  }
  /**
   * Applies a JSON Patch (RFC 6902) to an object represented by a string object.
   * (This could be used if you are still using Jackson 2.)
   * @param originalJsonAsString Original objekt
   * @param patchJson JSON Patch (Aaray of operations)
   * @param options  optional Patch-Options (e.g. blocked paths), null values allowed
   * @return Patched JSON as String
   * @throws JsonPatchProcessingException in case of processing errors
   */
  public static String applyPatch(String originalJsonAsString, String patchJson, PatchOptions options) throws  JsonPatchProcessingException {
    try {
      // validate patch
      if (options != null && !options.blockedPaths().isEmpty()) {
        validateBlockedPaths(patchJson, options.blockedPaths());
      }
      // apply patch (Jakarta JSON-P)
      logger.trace("Applying JsonPatch '{}' to JSON document '{}'.", patchJson, originalJsonAsString);
      JsonArray patchOps;
      try (JsonReader patchReader = Json.createReader(new StringReader(patchJson))) {
        JsonValue v = patchReader.readValue();
        if (v.getValueType() != JsonValue.ValueType.ARRAY) {
          throw new JsonPatchProcessingException("JSON Patch has to be an array (RFC 6902)\\n" +
                  "Offending content: " + patchJson + "\n" +
                  "Parsed value type: " + v.getValueType());
        }
        patchOps = v.asJsonArray();
      }

      jakarta.json.JsonPatch patch = Json.createPatch(patchOps);
      JsonStructure target;
      try (JsonReader originalReader = Json.createReader(new StringReader(originalJsonAsString))) {
        target = originalReader.read();
      }
      JsonStructure patched = patch.apply(target);
      logger.trace("Patched JSON document: '{}'.", patched);

      // JSON -> String
      return patched.toString();
    } catch (Exception e) {
      throw new JsonPatchProcessingException("Error while executing patch!", e);
    }
  }
  /**
   * Applies a JSON Patch (RFC 6902) to an object represented by a string object.
   * (This could be used if you are still using Jackson 2.)
   * @param originalJsonAsString Original objekt
   * @param patchJson JSON Patch (Aaray of operations)
   * @param options  optional Patch-Options (e.g. blocked paths), null values allowed
   * @return Patched JSON as String
   * @throws JsonPatchProcessingException in case of processing errors
   */
  public static String applyPatch(String originalJsonAsString, JsonPatch patchJson, PatchOptions options) throws  JsonPatchProcessingException {
    return applyPatch(originalJsonAsString, jsonObjectToString(patchJson), options);
  }
  /**
   * Applies a JSON Patch (RFC 6902) to an object represented by a string object.
   * (This could be used if you are still using Jackson 2.)
   * @param originalJsonAsString Original objekt
   * @param patchJson JSON Patch (Aaray of operations)
   * @param options  optional Patch-Options (e.g. blocked paths), null values allowed
   * @return Patched JSON as String
   * @throws JsonPatchProcessingException in case of processing errors
   */
  public static String applyPatch(String originalJsonAsString, JsonNode patchJson, PatchOptions options) throws   JsonPatchProcessingException {
    return applyPatch(originalJsonAsString, patchJson.toString(), options);
  }
  /** Applies a JSON Patch (RFC 6902) to a JsonNode.
   *
   * @param original Original JsonNode
   * @param jsonPatch JSON Patch as String (Array of operations)
   * @param options  optional Patch-Options (e.g. blocked paths), null values allowed
   * @return Patched JsonNode
   * @throws JsonPatchProcessingException in case of processing errors
   */
  public static JsonNode applyPatch(JsonNode original, String jsonPatch, PatchOptions options) throws  JsonPatchProcessingException {
    String patchedJson = applyPatch(original.toString(), jsonPatch, options);
    return jsonStringToNode(patchedJson);
  }
  /**
   * Applies a JSON Patch (RFC 6902) to a JsonNode.
   *
   * @param original Original JsonNode
   * @param jsonPatch JSON Patch as JsonPatch object
   * @param options  optional Patch-Options (e.g. blocked paths), null values allowed
   * @return Patched JsonNode
   * @throws JsonPatchProcessingException in case of processing errors
   */
  public static JsonNode applyPatch(JsonNode original, JsonPatch jsonPatch, PatchOptions options) {
    return applyPatch(original, jsonObjectToString(jsonPatch), options);
  }
  /**
   * Applies a JSON Patch (RFC 6902) to a JsonNode.
   *
   * @param original Original JsonNode
   * @param patch JSON Patch as JsonNode (Array of operations)
   * @param options  optional Patch-Options (e.g. blocked paths), null values allowed
   * @return Patched JsonNode
   * @throws JsonPatchProcessingException in case of processing errors
   */
  public static JsonNode applyPatch(JsonNode original, JsonNode patch, PatchOptions options) throws   JsonPatchProcessingException {
    return applyPatch(original, patch.toString(), options);
  }

    /**
     * Applies a JSON Patch (RFC 6902) to an object.
     *
     * @param original Original objekt
     * @param patchJson JSON Patch (Aaray of operations)
     * @param type target type
     * @param options optional Patch-Options (e.g. blocked paths), null values allowed
     * @return Patched object
     * @throws JsonPatchProcessingException if patch application fails
     */
  public static <T> T applyPatch(T original, String patchJson, Class<T> type, PatchOptions options) throws JsonPatchProcessingException {
    try {
      // POJO -> JSON
      String originalJsonAsString = jsonObjectToString(original);

      // patch original JSON
      String patchedJson = applyPatch(originalJsonAsString, patchJson, options);

      // 4) JSON -> POJO
      return jsonStringToObject(patchedJson, type);
    } catch (Exception e) {
      throw new JsonPatchProcessingException("Error while executing patch!", e);
    }
  }

  /** Applies a JSON Merge Patch (FFC 7396) on an object.
   * @param original Original object
   * @param mergePatchJson JSON Merge Patch (object)
   * @param type target type
   * @return Patched object
   * @throws JsonPatchProcessingException in case of processing errors
   */
  public static <T> T applyMergePatch(T original, String mergePatchJson, Class<T> type) throws  JsonPatchProcessingException {
    try {
      String originalJson = getDefaultMapper().writeValueAsString(original);
      String patchedJson = applyMergePatch(originalJson, mergePatchJson);
      return getDefaultMapper().readValue(patchedJson, type);
    } catch (Exception e) {
      throw new JsonPatchProcessingException("Error while applying JSON Merge Patch", e);
    }
  }

  /**
   * Applies a JSON Merge Patch (FFC 7396) on an object.
   * @param originalJsonAsString Original object
   * @param mergePatchJson JSON Merge Patch (object)
   */
  public static String applyMergePatch(String  originalJsonAsString, String mergePatchJson) throws   JsonPatchProcessingException {
    try {
      logger.trace("Applying JSON Merge Patch '{}' to JSON document '{}'.", mergePatchJson, originalJsonAsString);
      JsonMergePatch mergePatch;
      try (JsonReader reader = Json.createReader(new StringReader(mergePatchJson))) {
        JsonValue v = reader.readValue();
        // JSON Merge Patch is a JSON object (not an array)
        if (v.getValueType() != JsonValue.ValueType.OBJECT) {
          throw new JsonPatchProcessingException("JSON Merge Patch has to be an object (RFC 7396)");
        }
        mergePatch = Json.createMergePatch(v);
      }
      JsonStructure target;
      try (JsonReader originalReader = Json.createReader(new StringReader(originalJsonAsString))) {
        target = originalReader.read();
      }
      JsonValue patched = mergePatch.apply(target);
      logger.trace("Merged JSON document: '{}'.", patched);
      return patched.toString();
    } catch (Exception e) {
      throw new JsonPatchProcessingException("Error while applying JSON Merge Patch", e);
    }
  }

  /**
   * Check for updating blocked paths.
   * @param patchJson JSON Patch with operations
   * @param blockedPaths set of blocked paths
   */
  private static void validateBlockedPaths(String patchJson, Set<String> blockedPaths) throws  JsonPatchProcessingException {
    try (JsonReader patchReader = Json.createReader(new StringReader(patchJson))) {
      logger.trace("Checking for blocked paths '{}' in JSON patch '{}'.", blockedPaths, patchJson);
      JsonValue v = patchReader.readValue();
      if (v.getValueType() != JsonValue.ValueType.ARRAY) return; // other errors are handled elsewhere
      for (JsonValue op : v.asJsonArray()) {
        if (op.getValueType() == JsonValue.ValueType.OBJECT) {
          var obj = op.asJsonObject();
          String path = obj.getString("path", null);
          String from = obj.getString("from", null);
          if (path != null && blockedPaths.contains(path)) {
            throw new JsonPatchProcessingException("Path is read only and can't be changed: " + path);
          }
          if (from != null && blockedPaths.contains(from)) {
            throw new JsonPatchProcessingException("Path (from) is read only and can't be changed:: " + from);
          }
        }
      }
    }
  }
  /* Helper methods */
  /** Utility method to create a string from JsonPatch.
   * @param value JsonPatch
   * @return String representation
   */
  public static String jsonObjectToString(Object value) throws  JsonPatchProcessingException {
    try {
      return getDefaultMapper().writeValueAsString(value);
    } catch (Exception e) {
      throw new JsonPatchProcessingException("Error while serializing Pojo '" + value.toString() + "'!", e);
    }
  }
  /** Utility method to create a JsonNode from a string.
   * @param json JSON string
   * @return JsonNode
   */
  public static JsonNode jsonStringToNode(String json) throws  JsonPatchProcessingException {
    try {
      return getDefaultMapper().readTree(json);
    } catch (Exception e) {
      throw new JsonPatchProcessingException("Error while parsing JSON string!", e);
    }
  }
  /** Utility method to create an object from a JSON string.
   * @param json JSON string
   * @param type target type
   * @return Object of target type
   */
  public static <T> T jsonStringToObject(String json, Class<T> type) throws   JsonPatchProcessingException {
    try {
      return getDefaultMapper().readValue(json, type);
    } catch (Exception e) {
      throw new JsonPatchProcessingException("Error while parsing JSON string to object!", e);
    }
  }

  static void resetForTesting() {
    if (!Boolean.getBoolean("json.patch.resetForTesting")) {
      throw new IllegalStateException("Resetting JsonPatchUtil is only allowed in testing!");
    }
    synchronized (DEFAULT_MAPPER_CONFIGURATIONS) {
      DEFAULT_MAPPER_CONFIGURATIONS.clear();
      DEFAULT_MAPPER.set(null);
      frozen = false;
    }
  }
}
