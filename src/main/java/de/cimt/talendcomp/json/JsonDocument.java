/**
 * Copyright 2015 Jan Lolling jan.lolling@gmail.com
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.cimt.talendcomp.json;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ParseContext;
import com.jayway.jsonpath.PathNotFoundException;
import com.jayway.jsonpath.internal.JsonContext;
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;

/**
 * Convenient class to work with the Jackson-API for JSON
 */
public class JsonDocument {
	
	public static final String NULL_STRING = "null";
	public static final Integer NULL_INTEGER = Integer.MAX_VALUE;
	public static final Long NULL_LONG = Long.MAX_VALUE;
	public static final Double NULL_DOUBLE = Double.MAX_VALUE;
	public static final Float NULL_FLOAT = Float.MAX_VALUE;
	public static final Short NULL_SHORT = Short.MAX_VALUE;
	public static final BigDecimal NULL_BIGDECIMAL = new BigDecimal(Long.MAX_VALUE);
	public static final BigInteger NULL_BIGINTEGER = new BigInteger(String.valueOf(Long.MAX_VALUE)); 
	public static final Date NULL_DATE = new Date(Long.MAX_VALUE);
	
	private JsonNode rootNode = null;
	private ObjectMapper objectMapper = new ObjectMapper();
	private static final String DEFAULT_DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS";
	private Map<String, SimpleDateFormat> dateFormatMap = new HashMap<String, SimpleDateFormat>();
	private DocumentContext rootContext = null;
	private static final Configuration JACKSON_JSON_NODE_CONFIGURATION = Configuration
            .builder()
            .mappingProvider(new JacksonMappingProvider())
            .jsonProvider(new JacksonJsonNodeJsonProvider())
            .build();
	private ParseContext parseContext = JsonPath.using(JACKSON_JSON_NODE_CONFIGURATION);
	private Map<String, JsonPath> compiledPathMap = new HashMap<String, JsonPath>();
	private String currentPath = "";
	
	public JsonDocument(boolean isArray) {
		if (isArray) {
			rootContext = parseContext.parse("[]");
		} else {
			rootContext = parseContext.parse("{}");
		}
		rootNode = rootContext.read("$");
		JsonNode testNode = rootContext.read("$");
		if (rootNode != testNode) {
			throw new IllegalStateException("Clones objects detected! Use the latest Jayway library 2.2.1+");
		}
	}

	public JsonDocument(String jsonContent) {
		if (jsonContent != null && jsonContent.trim().isEmpty() == false) {
			rootContext = parseContext.parse(jsonContent);
		} else { 
			throw new IllegalArgumentException("Json input content cannot be empty or null");
		}
		rootNode = rootContext.read("$");
		JsonNode testNode = rootContext.read("$");
		if (rootNode != testNode) {
			throw new IllegalStateException("Clones objects detected! Use the latest Jayway library 2.2.1+");
		}
	}
	
	public JsonDocument(File jsonFile) throws Exception {
		if (jsonFile != null) {
			if (jsonFile.exists() == false) {
				throw new Exception("JSON input file: " + jsonFile.getAbsolutePath() + " does not exists or is not readable!");
			}
			InputStream in = new FileInputStream(jsonFile); 
			rootContext = parseContext.parse(in);
			// parseContext closes this stream
		} else { 
			throw new IllegalArgumentException("Json input input file cannot be null!");
		}
		rootNode = rootContext.read("$");
		JsonNode testNode = rootContext.read("$");
		if (rootNode != testNode) {
			throw new IllegalStateException("Clones objects detected! Use the latest Jayway library 2.2.1+");
		}
	}

	public JsonDocument(JsonNode jsonNode, boolean isArray) throws Exception {
		if (jsonNode != null) {
			rootContext = parseContext.parse("{}");
			rootContext.set("$", jsonNode);
		} else {
			if (isArray) {
				rootContext = parseContext.parse("[]");
			} else {
				rootContext = parseContext.parse("{}");
			}
		}
		rootNode = jsonNode;
		JsonNode testNode = rootContext.read("$");
		if (rootNode != testNode) {
			throw new IllegalStateException("Clones objects detected! Use the latest Jayway library 2.2.1+");
		}
	}

	public DocumentContext getDocumentContext() {
		return rootContext;
	}
	
	public boolean isArray() {
		return rootNode.isArray();
	}
	
	public ObjectNode createEmptyNode() {
		return objectMapper.createObjectNode();
	}
	
	public ObjectNode createObjectNode(String name) {
		ObjectNode child = ((ObjectNode) rootNode).objectNode();
		if (isArray()) {
			throw new IllegalStateException("Root is an array, use addObjectNode instead!");
		}
		((ObjectNode) rootNode).set(name, child);
		return child;
	}

	public ObjectNode addObjectNode(String name) {
		ObjectNode child = ((ArrayNode) rootNode).objectNode();
		if (isArray() == false) {
			throw new IllegalStateException("Root is an array, use addObjectNode instead!");
		}
		((ArrayNode) rootNode).add(child);
		return child;
	}

	public ObjectNode addObjectNode(JsonNode parent, String name) {
		if (parent instanceof ArrayNode) {
			ObjectNode child = ((ArrayNode) parent).objectNode();
			((ArrayNode) parent).add(child);
			return child;
		} else {
			throw new IllegalArgumentException("parent must be an ArrayNode!");
		}
	}

	public ArrayNode addArrayNode(JsonNode parent, String name) {
		if (parent instanceof ArrayNode) {
			ArrayNode child = ((ArrayNode) parent).arrayNode();
			((ArrayNode) parent).add(child);
			return child;
		} else {
			throw new IllegalArgumentException("parent must be an ArrayNode!");
		}
	}

	public ArrayNode createArrayNode(String name) {
		if (isArray()) {
			ArrayNode child = ((ArrayNode) rootNode).arrayNode();
			((ArrayNode) rootNode).add(child);
			return child;
		} else {
			ArrayNode child = ((ObjectNode) rootNode).arrayNode();
			((ObjectNode) rootNode).set(name, child);
			return child;
		}
	}

	@Override
	public String toString() {
		return rootNode.toString();
	}

	private JsonPath getCompiledJsonPath(String jsonPathStr) {
		JsonPath compiledPath = compiledPathMap.get(jsonPathStr);
		if (compiledPath == null) {
			compiledPath = JsonPath.compile(jsonPathStr);
			compiledPathMap.put(jsonPathStr, compiledPath);
		}
		return compiledPath;
	}
	
	/**
	 * returns the node start from the root
	 * @param jsonPath
	 * @return node or null if nothing found or a MissingNode was found
	 */
	public JsonNode getNode(String jsonPath) {
		try {
			JsonPath compiledPath = getCompiledJsonPath(jsonPath);
			rootContext.read(compiledPath);
			JsonNode node = rootContext.read(compiledPath);
			if (node.isMissingNode()) {
				return null;
			} else {
				return node;
			}
		} catch (PathNotFoundException e) {
			return null;
		}
	}
	
	/**
	 * Retrieve a node by direct-path jsonPath
	 * @param jsonPath must be a path with query parts and starts with root
	 * @param create if true, missing nodes will be created
	 * @return node
	 * @throws Exception 
	 */
	public JsonNode getNode(String jsonPath, boolean create) throws Exception {
		return getNode(rootNode, jsonPath, create);
	}
	
	/**
	 * Retrieves with real JSONPath the nodes starting from the given node
	 * @param parentNode
	 * @param jsonPath
	 * @return an ArrayNode with the search result
	 */
	public JsonNode getNode(JsonNode parentNode, String jsonPath) {
		if (jsonPath == null || jsonPath.trim().isEmpty()) {
			throw new IllegalArgumentException("jsonPath cannot be null or empty");
		}
		if (parentNode == null) {
			parentNode = rootNode;
		}
		if (parentNode == rootNode && jsonPath.startsWith("$")) {
			return getNode(jsonPath);
		}
		DocumentContext context = new JsonContext(JACKSON_JSON_NODE_CONFIGURATION).parse(parentNode);
		// fake a root path but use a arbitrary node as fake root
		JsonPath compiledPath = getCompiledJsonPath(jsonPath);
		return context.read(compiledPath);
	}
			
	/**
	 * Retrieve a node by direct-path jsonPath
	 * @param parentNode node to start
	 * @param jsonPath must be a path with query parts and starts with the given parent node
	 * @param create if true, missing nodes will be created otherwise it returns null
	 * @return node
	 */
	public JsonNode getNode(JsonNode parentNode, String jsonPath, boolean create) throws Exception {
		if (jsonPath == null || jsonPath.trim().isEmpty()) {
			throw new IllegalArgumentException("jsonPath cannot be null or empty");
		}
		if (parentNode == null) {
			parentNode = rootNode;
		}
		if (create == false) {
			if (parentNode == rootNode || jsonPath.startsWith("$")) {
				return getNode(jsonPath);
			} else {
				return getNode(parentNode, jsonPath);
			}
		} else {
			JsonNode childNode = null;
			if (jsonPath == null || jsonPath.trim().isEmpty() || jsonPath.trim().equals(".")) {
				return parentNode;
			}
			// starting from the given parentNode we search the childNode
			// $.bo.person[1].address[2][3].street[4]
			List<PathToken> listTokens = PathToken.parse(jsonPath);
			for (PathToken t : listTokens) {
				childNode = null;
				if (parentNode instanceof ObjectNode) {
					if (t instanceof AttributeToken) {
						String name = ((AttributeToken) t).getName();
						// check if there is a NullNode -> withArray does not work in this case
						childNode = ((ObjectNode) parentNode).get(((AttributeToken) t).getName());
						if (childNode instanceof NullNode) {
							((ObjectNode) parentNode).remove(((AttributeToken) t).getName());
						}
						if (t.isNextTokenArray()) {
							// setup an array
							childNode = ((ObjectNode) parentNode).withArray(name);
						} else {
							// setup an object
							childNode = ((ObjectNode) parentNode).with(name);
						}
					} else if (t instanceof ArrayToken) {
						// we have a ObjectNode and expect to get an node from an array
						// we are wrong here
						throw new Exception("The jsonpath expects an array node but found an object node at: "+ t.getPath());
					}
				} else if (parentNode instanceof ArrayNode) {
					if (t instanceof ArrayToken) {
						childNode = ((ArrayNode) parentNode).get(((ArrayToken) t).getIndex());
						if (childNode == null || childNode.isNull()) {
							if (t.isNextTokenArray()) {
								childNode = ((ArrayNode) parentNode).addArray();
							} else {
								childNode = ((ArrayNode) parentNode).addObject();
							}
						}
					} else if (t instanceof AttributeToken) {
						// we have a ObjectNode and expect to get an node from an array
						// we are wrong here
						throw new Exception("The jsonpath expects an object node but found an array node at: "+ t.getPath());
					}
				}
				if (childNode != null) {
					parentNode = childNode;
				} else {
					break;
				}
			}
			return childNode;
		}
	}
	
	public DocumentContext getRootContext() {
		return rootContext;
	}
	
	public JsonNode getRootNode() {
		return rootNode;
	}
	
	public String formatDate(Date value, String pattern) {
		if (value == null) {
			return null;
		}
		if (pattern == null || pattern.trim().isEmpty()) {
			pattern = DEFAULT_DATE_PATTERN;
		}
		SimpleDateFormat sdf = dateFormatMap.get(pattern);
		if (sdf == null) {
			sdf = new SimpleDateFormat(pattern);
			dateFormatMap.put(pattern, sdf);
		}
		return sdf.format(value);
	}

	public Date parseDate(String value, String pattern) throws ParseException {
		if (value == null) {
			return null;
		}
		if (pattern == null || pattern.trim().isEmpty()) {
			pattern = DEFAULT_DATE_PATTERN;
		}
		SimpleDateFormat sdf = dateFormatMap.get(pattern);
		if (sdf == null) {
			sdf = new SimpleDateFormat(pattern);
			dateFormatMap.put(pattern, sdf);
		}
		return sdf.parse(value);
	}

	public JsonNode setJsonObject(ObjectNode node, String fieldName, String json) throws Exception {
		return setJsonObject(node, fieldName, json, false);
	}

	public JsonNode setJsonObject(ObjectNode node, String fieldName, String json, boolean omitAttrIfNullValue) throws Exception {
		if (omitAttrIfNullValue && json == null) {
			return node;
		}
		JsonNode newnode = buildNode(json);
		node.set(fieldName, newnode);
		return node;
	}

	public JsonNode setJsonObject(ObjectNode node, String fieldName, JsonNode json) throws Exception {
		return setJsonObject(node, fieldName, json, false);
	}

	public JsonNode setJsonObject(ObjectNode node, String fieldName, JsonNode json, boolean omitAttrIfNullValue) throws Exception {
		if (omitAttrIfNullValue && json == null) {
			return node;
		}
		node.set(fieldName, json);
		return node;
	}

	public ObjectNode setValue(ObjectNode node, String fieldName, String value) {
		return setValue(node, fieldName, value, false);
	}
	
	public ObjectNode setValue(ObjectNode node, String fieldName, String value, boolean omitAttrIfNullValue) {
		if (omitAttrIfNullValue && value == null) {
			return node;
		}
		node.put(fieldName, value);
		return node;
	}

	public ObjectNode setValue(ObjectNode node, String fieldName, Boolean value) {
		return setValue(node, fieldName, value, false);
	}

	public ObjectNode setValue(ObjectNode node, String fieldName, Boolean value, boolean omitAttrIfNullValue) {
		if (omitAttrIfNullValue && value == null) {
			return node;
		}
		node.put(fieldName, value);
		return node;
	}

	public ObjectNode setValue(ObjectNode node, String fieldName, Integer value) {
		return setValue(node, fieldName, value, false);
	}

	public ObjectNode setValue(ObjectNode node, String fieldName, Integer value, boolean omitAttrIfNullValue) {
		if (omitAttrIfNullValue && value == null) {
			return node;
		}
		node.put(fieldName, value);
		return node;
	}

	public ObjectNode setValue(ObjectNode node, String fieldName, Long value) {
		return setValue(node, fieldName, value, false);
	}

	public ObjectNode setValue(ObjectNode node, String fieldName, Long value, boolean omitAttrIfNullValue) {
		if (omitAttrIfNullValue && value == null) {
			return node;
		}
		node.put(fieldName, value);
		return node;
	}

	public ObjectNode setValue(ObjectNode node, String fieldName, BigDecimal value) {
		return setValue(node, fieldName, value, false);
	}

	public ObjectNode setValue(ObjectNode node, String fieldName, BigDecimal value, boolean omitAttrIfNullValue) {
		if (omitAttrIfNullValue && value == null) {
			return node;
		}
		node.put(fieldName, value);
		return node;
	}

	public ObjectNode setValue(ObjectNode node, String fieldName, BigInteger value) {
		return setValue(node, fieldName, value, false);
	}

	public ObjectNode setValue(ObjectNode node, String fieldName, BigInteger value, boolean omitAttrIfNullValue) {
		if (omitAttrIfNullValue && value == null) {
			return node;
		}
		node.put(fieldName, value);
		return node;
	}

	public ObjectNode setValue(ObjectNode node, String fieldName, Double value) {
		return setValue(node, fieldName, value, false);
	}

	public ObjectNode setValue(ObjectNode node, String fieldName, Double value, boolean omitAttrIfNullValue) {
		if (omitAttrIfNullValue && value == null) {
			return node;
		}
		node.put(fieldName, value);
		return node;
	}

	public ObjectNode setValue(ObjectNode node, String fieldName, Float value) {
		return setValue(node, fieldName, value, false);
	}

	public ObjectNode setValue(ObjectNode node, String fieldName, Float value, boolean omitAttrIfNullValue) {
		if (omitAttrIfNullValue && value == null) {
			return node;
		}
		node.put(fieldName, value);
		return node;
	}

	public ObjectNode setValue(ObjectNode node, String fieldName, Short value) {
		return setValue(node, fieldName, value, false);
	}

	public ObjectNode setValue(ObjectNode node, String fieldName, Short value, boolean omitAttrIfNullValue) {
		if (omitAttrIfNullValue && value == null) {
			return node;
		}
		node.put(fieldName, value);
		return node;
	}

	public ObjectNode setValue(ObjectNode node, String fieldName, Date value, String pattern) {
		return setValue(node, fieldName, value, pattern, false);
	}
	
	public ObjectNode setValue(ObjectNode node, String fieldName, Date value, String pattern, boolean omitAttrIfNullValue) {
		if (omitAttrIfNullValue && value == null) {
			return node;
		}
		node.put(fieldName, formatDate(value, pattern));
		return node;
	}

	public JsonNode buildNode(Object value) throws Exception {
		if (value instanceof String) {
			String jsonString = (String) value;
			if (jsonString == null || jsonString.trim().isEmpty()) {
				return null;
			} else {
				return objectMapper.readTree(jsonString);
			}
		} else if (value instanceof JsonNode) {
			return (JsonNode) value;
		} else {
			return null;
		}
	}
	
	public String getJsonString(boolean prettyPrint, boolean suppressEmpty) throws JsonProcessingException {
		if (suppressEmpty) {
			objectMapper.setSerializationInclusion(Include.NON_EMPTY);
		} else {
			objectMapper.setSerializationInclusion(Include.ALWAYS);
		}
		if (prettyPrint) {
			return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode);
		} else {
			return objectMapper.writeValueAsString(rootNode);
		}
	}
	
	public void writeToFile(String filePath, boolean prettyPrint, boolean suppressEmpty) throws Exception {
		if (filePath == null || filePath.trim().isEmpty()) {
			throw new IllegalArgumentException("Output file path cannot be null or empty!");
		}
		File file = new File(filePath);
		File dir = file.getParentFile();
		if (dir.exists() == false) {
			dir.mkdirs();
		}
		if (dir.exists() == false) {
			throw new Exception("Cannot create output dir: " + dir.getAbsolutePath());
		}
		BufferedWriter br = null;
		try {
			br = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));
			br.write(getJsonString(prettyPrint, suppressEmpty));
			br.flush();
		} finally {
			if (br != null) {
				br.close();
			}
		}
	}
	
	/**
	 * Returns the value of the addressed field
	 * @param node node containing the field 
	 * @param fieldName attribute name
	 * @param isNullable 
	 * @param dummy only to become compatible with the other methods
	 * @return if node is an ObjectNode: the value of the attribute, if node is a value node: the node itself, if the fieldName == ".": the node itself
	 * @throws Exception if iNullable == false and the attribute does not exists or is null or the node is null.
	 */
	public JsonNode getValueAsObject(JsonNode node, String fieldName, boolean isNullable, boolean allowMissing, Object dummy) throws Exception {
		if (node != null) {
			JsonNode valueNode = null;
			if (fieldName == null || ".".equals(fieldName) || node.isValueNode()) {
				valueNode = node;
			} else {
				valueNode = node.path(fieldName);
				if (isNullable == false && valueNode != null && valueNode.isNull()) {
					throw new Exception(currentPath + ": Attribute: " + fieldName + ": value is null but configured as not-nullable!");
				}
				if (allowMissing == false && valueNode != null && valueNode.isMissingNode()) {
					throw new Exception(currentPath + ": Attribute: " + fieldName + " is missing but mandatory!");
				}
			}
			return valueNode;
		} else if (isNullable == false) {
			throw new Exception(currentPath + ": Parent node does not exists.");
		}
		return null;
	}
	
	/**
	 * Returns the value of the addressed field
	 * @param node node containing the field 
	 * @param fieldName attribute name
	 * @param isNullable 
	 * @param dummy only to become compatible with the other methods
	 * @return if node is an ObjectNode: the value of the attribute, if node is a value node: the node itself, if the fieldName == ".": the node itself
	 * @throws Exception if iNullable == false and the attribute does not exists or is null or the node is null.
	 */
	private JsonNode getValueNode(JsonNode node, String fieldName, boolean isNullable, boolean allowMissing) throws Exception {
		if (node != null) {
			JsonNode valueNode = null;
			if (fieldName == null || ".".equals(fieldName) || node.isValueNode()) {
				valueNode = node;
				if (valueNode != null && valueNode.isNull()) {
					if (isNullable == false) {
						throw new Exception(currentPath + ": Value of the given node is null but configured as not-nullable!");
					} else {
						valueNode = null;
					}
				}
			} else {
				valueNode = node.path(fieldName);
				if (valueNode != null && valueNode.isNull()) {
					if (isNullable == false) {
						throw new Exception(currentPath + ": Attribute: " + fieldName + ": value is null but configured as not-nullable!");
					} else {
						valueNode = null;
					}
				}
				if (allowMissing == false && valueNode != null && valueNode.isMissingNode()) {
					throw new Exception(currentPath + ": Attribute: " + fieldName + " is missing but mandatory!");
				}
			}
			return valueNode;
		} else if (isNullable == false) {
			throw new Exception(currentPath + ": Parent node does not exists.");
		} else {
			return null;
		}
	}

	public String getArrayValuesAsChain(ArrayNode arrayNode) {
		StringBuilder sb = new StringBuilder();
		if (arrayNode != null) {
			boolean firstLoop = true;
			for (JsonNode valueNode : arrayNode.elements().next()) {
				if (valueNode.isValueNode()) {
					String value = valueNode.asText();
					if (value != null && value.isEmpty() == false) {
						if (firstLoop) {
							firstLoop = false;
						} else {
							sb.append(",");
						}
						sb.append(value);
					}
				}
			}
		}
		return sb.toString();
	}
	
	public List<JsonNode> getArrayValuesAsList(JsonNode node) {
		List<JsonNode> result = new ArrayList<JsonNode>();
		if (node instanceof ArrayNode) {
			ArrayNode arrayNode = (ArrayNode) node;
			if (arrayNode != null) {
				for (JsonNode childNode : arrayNode) {
					result.add(childNode);
				}
			}
		} else if (node != null) {
			result.add(node);
		}
		return result;
	}
	
	public String getValueAsString(JsonNode node, String fieldName, boolean isNullable, boolean allowMissing, String missingNodeValue) throws Exception {
		JsonNode valueNode = getValueNode(node, fieldName, isNullable, allowMissing);
		if (valueNode != null) {
			if (valueNode.isMissingNode()) {
				if (isNullable == false && missingNodeValue == null) {
					throw new Exception(currentPath + ": Attribute: " + fieldName + " is missing and configured as not-nullable but the replacement value is also null!");
				} else {
					return missingNodeValue;
				}
			}
			if (valueNode.isArray()) {
				return getArrayValuesAsChain((ArrayNode) valueNode);
			} else {
				if (valueNode.isValueNode()) {
					return valueNode.asText();
				} else {
					return valueNode.toString();
				}
			}
		} else {
			return null;
		}
	}

	public Boolean getValueAsBoolean(JsonNode node, String fieldName, boolean isNullable, boolean allowMissing, Boolean missingNodeValue) throws Exception {
		JsonNode valueNode = getValueNode(node, fieldName, isNullable, allowMissing);
		if (valueNode != null) {
			if (valueNode.isMissingNode()) {
				if (isNullable == false && missingNodeValue == null) {
					throw new Exception(currentPath + ": Attribute: " + fieldName + " is missing and configured as not-nullable but the replacement value is also null!");
				} else {
					return missingNodeValue;
				}
			}
			return valueNode.asBoolean();
		} else {
			return null;
		}
	}

	public Integer getValueAsInteger(JsonNode node, String fieldName, boolean isNullable, boolean allowMissing, Integer missingNodeValue) throws Exception {
		JsonNode valueNode = getValueNode(node, fieldName, isNullable, allowMissing);
		if (valueNode != null) {
			if (valueNode.isMissingNode()) {
				if (isNullable == false && missingNodeValue == null) {
					throw new Exception(currentPath + ": Attribute: " + fieldName + " is missing and configured as not-nullable but the replacement value is also null!");
				} else {
					return missingNodeValue;
				}
			}
			return valueNode.asInt();
		} else {
			return null;
		}
	}
	
	public Long getValueAsLong(JsonNode node, String fieldName, boolean isNullable, boolean allowMissing, Long missingNodeValue) throws Exception {
		JsonNode valueNode = getValueNode(node, fieldName, isNullable, allowMissing);
		if (valueNode != null) {
			if (valueNode.isMissingNode()) {
				if (isNullable == false && missingNodeValue == null) {
					throw new Exception(currentPath + ": Attribute: " + fieldName + " is missing and configured as not-nullable but the replacement value is also null!");
				} else {
					return missingNodeValue;
				}
			}
			return valueNode.asLong();
		} else {
			return null;
		}
	}

	public Double getValueAsDouble(JsonNode node, String fieldName, boolean isNullable, boolean allowMissing, Double missingNodeValue) throws Exception {
		JsonNode valueNode = getValueNode(node, fieldName, isNullable, allowMissing);
		if (valueNode != null) {
			if (valueNode.isMissingNode()) {
				if (isNullable == false && missingNodeValue == null) {
					throw new Exception(currentPath + ": Attribute: " + fieldName + " is missing and configured as not-nullable but the replacement value is also null!");
				} else {
					return missingNodeValue;
				}
			}
			return valueNode.asDouble();
		} else {
			return null;
		}
	}

	public Float getValueAsFloat(JsonNode node, String fieldName, boolean isNullable, boolean allowMissing, Float missingNodeValue) throws Exception {
		JsonNode valueNode = getValueNode(node, fieldName, isNullable, allowMissing);
		if (valueNode != null) {
			if (valueNode.isMissingNode()) {
				if (isNullable == false && missingNodeValue == null) {
					throw new Exception(currentPath + ": Attribute: " + fieldName + " is missing and configured as not-nullable but the replacement value is also null!");
				} else {
					return missingNodeValue;
				}
			}
			return (float) valueNode.asDouble();
		} else {
			return null;
		}
	}

	public Short getValueAsShort(JsonNode node, String fieldName, boolean isNullable, boolean allowMissing, Short missingNodeValue) throws Exception {
		JsonNode valueNode = getValueNode(node, fieldName, isNullable, allowMissing);
		if (valueNode != null) {
			if (valueNode.isMissingNode()) {
				if (isNullable == false && missingNodeValue == null) {
					throw new Exception(currentPath + ": Attribute: " + fieldName + " is missing and configured as not-nullable but the replacement value is also null!");
				} else {
					return missingNodeValue;
				}
			}
			return (short) valueNode.asInt();
		} else {
			return null;
		}
	}

	public BigDecimal getValueAsBigDecimal(JsonNode node, String fieldName, boolean isNullable, boolean allowMissing, BigDecimal missingNodeValue) throws Exception {
		JsonNode valueNode = getValueNode(node, fieldName, isNullable, allowMissing);
		if (valueNode != null) {
			if (valueNode.isMissingNode()) {
				if (isNullable == false && missingNodeValue == null) {
					throw new Exception(currentPath + ": Attribute: " + fieldName + " is missing and configured as not-nullable but the replacement value is also null!");
				} else {
					return missingNodeValue;
				}
			}
			return new BigDecimal(valueNode.asText());
		} else {
			return null;
		}
	}

	public BigDecimal getValueAsBigDecimal(JsonNode node, String fieldName, boolean isNullable, boolean allowMissing, String missingNodeValue) throws Exception {
		JsonNode valueNode = getValueNode(node, fieldName, isNullable, allowMissing);
		if (valueNode != null) {
			if (valueNode.isMissingNode()) {
				if (missingNodeValue == null || missingNodeValue.trim().isEmpty()) {
					if (isNullable) {
						throw new Exception(currentPath + ": Attribute: " + fieldName + " is missing and configured as not-nullable but the replacement value is also null or empty!");
					} else {
						return null;
					}
				} else {
					return new BigDecimal(missingNodeValue);
				}
			}
			return new BigDecimal(valueNode.asText());
		} else {
			return null;
		}
	}

	public BigInteger getValueAsBigInteger(JsonNode node, String fieldName, boolean isNullable, boolean allowMissing, BigInteger missingNodeValue) throws Exception {
		JsonNode valueNode = getValueNode(node, fieldName, isNullable, allowMissing);
		if (valueNode != null) {
			if (valueNode.isMissingNode()) {
				if (isNullable == false && missingNodeValue == null) {
					throw new Exception(currentPath + ": Attribute: " + fieldName + " is missing and configured as not-nullable but the replacement value is also null!");
				} else {
					return missingNodeValue;
				}
			}
			return valueNode.bigIntegerValue();
		} else {
			return null;
		}
	}

	public Date getValueAsDate(JsonNode node, String fieldName, boolean isNullable, boolean allowMissing, Date missingNodeValue, String pattern) throws Exception {
		JsonNode valueNode = getValueNode(node, fieldName, isNullable, allowMissing);
		if (valueNode != null) {
			if (valueNode.isMissingNode()) {
				if (isNullable == false && missingNodeValue == null) {
					throw new Exception(currentPath + ": Attribute: " + fieldName + " is missing and configured as not-nullable but the replacement value is also null!");
				} else {
					return missingNodeValue;
				}
			}
			return parseDate(valueNode.asText(), pattern);
		} else {
			return null;
		}
	}

	public static String escape(String value) {
		if (value == null) {
			return null;
		} else if (value.isEmpty()) {
			return "";
		} else {
			StringBuilder sb = new StringBuilder();
			char c;
			int n = value.length();
			for (int i = 0; i < n; i++) {
				c = value.charAt(i);
				switch (c) {
					case '\n': sb.append("\\n"); break;
					case '\t': sb.append("\\t"); break;
					case '\"': sb.append("\\\""); break;
					case '\\': sb.append("\\\\"); break;
					case '\r': sb.append("\\r"); break;
					case '\f': sb.append("\\f"); break;
					case '\b': sb.append("\\b"); break;
					default: sb.append(c);
				}
			}
			return sb.toString();
		}
	}

	public String getCurrentPath() {
		return currentPath;
	}

	public void setCurrentPath(String currentPath) {
		if (currentPath == null) {
			currentPath = "Unknown path";
		}
		this.currentPath = currentPath;
	}
	
	public static int countNotNullAttributes(JsonNode node) {
		if (node == null) {
			throw new IllegalArgumentException("Node is null");
		} else if (node.size() == 0) {
			return 0;
		} else {
			int count = 0;
			Iterator<JsonNode> it = node.elements();
			while (it.hasNext()) {
				JsonNode valueNode = it.next();
				if (valueNode.isMissingNode() == false && valueNode.isNull() == false) {
					count++;
				}
			}
			return count;
		}
	}
	
}