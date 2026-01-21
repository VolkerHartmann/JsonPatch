[![Build Status](https://github.com/kit-data-manager/JsonPatch/actions/workflows/gradle.yml/badge.svg)](https://github.com/kit-data-manager/JsonPatch/actions/workflows/gradle.yml)
[![Codecov](https://codecov.io/gh/kit-data-manager/JsonPatch/graph/badge.svg)](https://codecov.io/gh/kit-data-manager/JsonPatch)
[![CodeQL](https://github.com/kit-data-manager/JsonPatch/actions/workflows/codeql-analysis.yml/badge.svg)](https://github.com/kit-data-manager/JsonPatch/actions/workflows/codeql-analysis.yml)
![License](https://img.shields.io/github/license/kit-data-manager/JsonPatch.svg)
![current Version](https://img.shields.io/github/v/release/kit-data-manager/JsonPatch)

# JSON Patch for Jackson 3

A small Java library that applies JSON Patch (RFC 6902) and JSON Merge Patch (RFC 7396) using Jakarta JSON-P and maps the results to POJOs with Jackson 3 (for example in Spring Boot 4).

## Why this approach?
- Jackson 3 introduced package and artifact name changes (for example, `tools.jackson.*`). Many existing JSON patch libraries are still tied to Jackson 2. This library avoids package conflicts by applying patches with Jakarta JSON-P and mapping results to POJOs using Jackson 3.
- The project supports the standardized formats RFC 6902 (JSON Patch) and RFC 7396 (JSON Merge Patch).

## Local build
From the project root run:

```bash
./gradlew build
./gradlew test
./gradlew publishToMavenLocal
```

## Usage
The library is available on Maven Central.

### Maven (pom.xml)

```xml
<dependency>
  <groupId>edu.kit.datamanager</groupId>
  <artifactId>JsonPatch</artifactId>
  <version>0.9.0</version>
</dependency>
```

### Gradle (build.gradle)

```groovy
implementation 'edu.kit.datamanager:JsonPatch:0.9.0'
```

## Examples
Below are two compact examples: 
1) build a `JsonPatch` programmatically and apply it to a `JsonNode`,
2) apply a patch to a POJO.

### 1) Build a JsonPatch and apply it to a JsonNode

```java
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import edu.kit.datamanager.util.json.JsonPatch;
import edu.kit.datamanager.util.json.JsonPatchUtil;
import java.util.List;

JsonMapper mapper = JsonMapper.builder().build();
JsonNode original = mapper.readTree("{\"name\":\"Alice\",\"age\":30}");

List<JsonPatch.Operation> ops = List.of(
  new JsonPatch.Operation("replace", "/name", null, "Bob"),
  new JsonPatch.Operation("add", "/city", null, "Karlsruhe")
);

JsonPatch patch = new JsonPatch(ops);
JsonNode patched = PatchUtil.applyPatch(original, patch);
System.out.println(patched.toPrettyString());
```

### 2) Apply a patch to a POJO

```java
import tools.jackson.databind.json.JsonMapper;
import edu.kit.datamanager.util.json.JsonPatchUtil;

public static class Person { public String name; public int age; }

JsonMapper mapper = JsonMapper.builder().build();
Person original = new Person(); 
original.name = "Alice"; 
original.age = 30;

String patchJson = "[ { \"op\": \"replace\", \"path\": \"/name\", \"value\": \"Bob\" } ]";

PatchUtil util = new PatchUtil(mapper);
Person patched = util.applyJsonPatch(original, patchJson, Person.class);
System.out.println(patched.name); // Bob
```

Note: the library uses Jakarta JSON-P for the actual patch application and Jackson 3 for mapping between JSON and POJOs.

## RFC 6902 vs RFC 7396 — when to use which

Short summary:
- JSON Patch (RFC 6902) is an array of operations (add, remove, replace, move, copy, test). Use it when you need fine-grained, ordered changes or array manipulations.
- JSON Merge Patch (RFC 7396) is a single JSON object that describes field replacements or deletions (setting a field to `null` removes it). Use it for straightforward partial updates.

### Example original JSON

```json
{
  "name": "Alice",
  "age": 30,
  "addr": { "city": "OldTown", "zip": "12345" },
  "tags": ["a","b"]
}
```

### JSON Patch (RFC 6902) — example

```json
[
  { "op": "replace", "path": "/name", "value": "Bob" },
  { "op": "add", "path": "/addr/street", "value": "Main St" },
  { "op": "remove", "path": "/age" },
  { "op": "add", "path": "/tags/1", "value": "x" }
]
```

Result: `name` becomes "Bob", `addr.street` is set, `age` is removed, `tags` becomes `["a","x","b"]`.

### JSON Merge Patch (RFC 7396) — example

```json
{
  "name": "Bob",
  "addr": { "city": "NewTown" },
  "tags": null
}
```

Semantics: `name` is replaced, `addr.city` is updated (recursive merge), and `tags: null` removes the `tags` field entirely.

Notes about this library (`PatchUtil`):
- `applyJsonPatch(...)` expects a JSON array (RFC 6902) and throws `JsonPatchProcessingException` if another format is supplied.
- `applyJsonMergePatch(...)` expects a JSON object (RFC 7396) and throws `JsonPatchProcessingException` if another format is supplied.
- `PatchOptions.ofBlockedPaths(...)` can be used with RFC 6902 patches to reject changes to blocked paths.

## Quick code reference

```java
// RFC6902
JsonPatch patch = new JsonPatch(List.of(new JsonPatch.Operation("replace", "/name", null, "Bob")));
JsonNode patched = PatchUtil.applyPatch(originalNode, patch);

// RFC7396
String mergePatch = "{ \"name\":\"Bob\", \"tags\": null }";
String result = new PatchUtil(mapper).applyJsonMergePatch(originalJsonString, mergePatch);
```

## License
Apache License 2.0 — see the LICENSE file.

## Acknowledgements

This work has been supported by the research program [‘Engineering Digital Futures’](https://www.helmholtz.de/en/research/research-fields/information/engineering-digital-futures/) of the [Helmholtz Association of German Research Centers](https://www.helmholtz.de/en) and the [Helmholtz Metadata Collaboration Platform (HMC)](https://helmholtz-metadaten.de/).
