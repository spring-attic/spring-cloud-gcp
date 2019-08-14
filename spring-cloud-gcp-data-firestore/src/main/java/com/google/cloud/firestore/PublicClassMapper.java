/*
 * Copyright 2019-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.firestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.cloud.Timestamp;
import com.google.firestore.v1.Document;
import com.google.firestore.v1.Value;

/**
 *
 * Temporary class to expose package-private methods, will be removed in the future.
 *
 * @author Dmitry Solomakha
 *
 */
public final class PublicClassMapper {

	private PublicClassMapper() {
	}

	public static Map<String, Value> convertToFirestoreTypes(Object update) {
		Map<String, Object> values = (Map<String, Object>) CustomClassMapper
				.convertToPlainJavaTypes(update);
		Map<String, Value> fields = new HashMap<>();

		for (Map.Entry<String, Object> entry : values.entrySet()) {
			Value encodedValue = UserDataConverter.encodeValue(FieldPath.of(entry.getKey()), entry.getValue(),
					UserDataConverter.ALLOW_ALL_DELETES);
			if (encodedValue != null) {
				fields.put(entry.getKey(), encodedValue);
			}
		}
		return fields;
	}

	public static <T> T convertToCustomClass(Document doc, Class<T> clazz) {
		Map<String, Value> fields = doc.getFieldsMap();
		Map<String, Object> decodedFields = new HashMap<>();
		for (Map.Entry<String, Value> entry : fields.entrySet()) {
			Object decodedValue = decodeValue(entry.getValue());
			decodedValue = convertToDateIfNecessary(decodedValue);
			decodedFields.put(entry.getKey(), decodedValue);
		}
		return CustomClassMapper.convertToCustomClass(decodedFields, clazz,
				new DocumentReference(null, ResourcePath.create(doc.getName())));
	}

	private static Object decodeValue(Value v) {
		Value.ValueTypeCase typeCase = v.getValueTypeCase();
		switch (typeCase) {
		case NULL_VALUE:
			return null;
		case BOOLEAN_VALUE:
			return v.getBooleanValue();
		case INTEGER_VALUE:
			return v.getIntegerValue();
		case DOUBLE_VALUE:
			return v.getDoubleValue();
		case TIMESTAMP_VALUE:
			return Timestamp.fromProto(v.getTimestampValue());
		case STRING_VALUE:
			return v.getStringValue();
		case BYTES_VALUE:
			return Blob.fromByteString(v.getBytesValue());
		// case REFERENCE_VALUE:
		// String pathName = v.getReferenceValue();
		// return new DocumentReference(firestore, ResourcePath.create(pathName));
		case GEO_POINT_VALUE:
			return new GeoPoint(
					v.getGeoPointValue().getLatitude(), v.getGeoPointValue().getLongitude());
		case ARRAY_VALUE:
			List<Object> list = new ArrayList<>();
			List<Value> lv = v.getArrayValue().getValuesList();
			for (Value iv : lv) {
				list.add(decodeValue(iv));
			}
			return list;
		case MAP_VALUE:
			Map<String, Object> outputMap = new HashMap<>();
			Map<String, Value> inputMap = v.getMapValue().getFieldsMap();
			for (Map.Entry<String, Value> entry : inputMap.entrySet()) {
				outputMap.put(entry.getKey(), decodeValue(entry.getValue()));
			}
			return outputMap;
		default:
			throw FirestoreException.invalidState(String.format("Unknown Value Type: %s", typeCase));
		}
	}

	private static Object convertToDateIfNecessary(Object decodedValue) {
		// if (decodedValue instanceof Timestamp) {
		// if (!this.firestore.areTimestampsInSnapshotsEnabled()) {
		// decodedValue = ((Timestamp) decodedValue).toDate();
		// }
		// }
		return decodedValue;
	}
}
