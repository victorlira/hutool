/*
 * Copyright (c) 2013-2024 Hutool Team and hutool.cn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.dromara.hutool.json.serializer;

import org.dromara.hutool.core.lang.Opt;
import org.dromara.hutool.core.lang.mutable.MutableEntry;
import org.dromara.hutool.core.text.StrUtil;
import org.dromara.hutool.core.util.ObjUtil;
import org.dromara.hutool.json.*;
import org.dromara.hutool.json.reader.JSONParser;
import org.dromara.hutool.json.reader.JSONTokener;
import org.dromara.hutool.json.xml.JSONXMLParser;
import org.dromara.hutool.json.xml.ParseConfig;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * 对象和JSON值映射器，用于Java对象和JSON对象互转<br>
 * <ul>
 *     <li>Java对象转JSON：{@link #map(Object)}</li>
 *     <li>JSON转Java对象：{@link #toBean(JSON, Type)}</li>
 * </ul>
 * <p>
 * 转换依赖于{@link JSONSerializer}和{@link JSONDeserializer}的实现，通过{@link TypeAdapterManager}统一管理<br>
 * 序列化和反序列化定义于两个作用域，首先查找本类中定义的，如果没有，使用{@link TypeAdapterManager#getInstance()} 查找全局定义的。
 *
 * @author looly
 * @since 6.0.0
 */
public class JSONMapper implements Serializable {

	private static final long serialVersionUID = -6714488573738940582L;

	/**
	 * 创建ObjectMapper
	 *
	 * @param jsonConfig 来源对象
	 * @param predicate  键值对过滤编辑器，可以通过实现此接口，完成解析前对键值对的过滤和修改操作，{@link Predicate#test(Object)}为{@code true}保留
	 * @return ObjectMapper
	 */
	public static JSONMapper of(final JSONConfig jsonConfig, final Predicate<MutableEntry<Object, Object>> predicate) {
		return new JSONMapper(jsonConfig, predicate);
	}

	private final JSONConfig jsonConfig;
	private final Predicate<MutableEntry<Object, Object>> predicate;
	private TypeAdapterManager typeAdapterManager;

	/**
	 * 构造
	 *
	 * @param jsonConfig JSON配置
	 * @param predicate  键值对过滤编辑器，可以通过实现此接口，完成解析前对键值对的过滤和修改操作，{@link Predicate#test(Object)}为{@code true}保留
	 */
	public JSONMapper(final JSONConfig jsonConfig, final Predicate<MutableEntry<Object, Object>> predicate) {
		this.jsonConfig = ObjUtil.defaultIfNull(jsonConfig, JSONConfig::of);
		this.predicate = predicate;
	}

	/**
	 * 获取自定义类型转换器，用于将自定义类型转换为JSONObject
	 *
	 * @return 类型转换器管理器
	 */
	public TypeAdapterManager getTypeAdapterManager() {
		return this.typeAdapterManager;
	}

	/**
	 * 设置自定义类型转换器，用于将自定义类型转换为JSONObject
	 *
	 * @param typeAdapterManager 类型转换器管理器
	 * @return this
	 */
	public JSONMapper setTypeAdapterManager(final TypeAdapterManager typeAdapterManager) {
		this.typeAdapterManager = typeAdapterManager;
		return this;
	}

	/**
	 * 转为实体类对象
	 *
	 * @param <T>  Bean类型
	 * @param json JSON
	 * @param type {@link Type}
	 * @return 实体类对象
	 */
	@SuppressWarnings("unchecked")
	public <T> T toBean(final JSON json, final Type type) {
		if (null == type || Object.class == type) {
			if (json instanceof JSONPrimitive) {
				return (T) ((JSONPrimitive) json).getValue();
			}
			return (T) this;
		}

		JSONDeserializer<Object> deserializer = null;
		// 自定义反序列化
		if (null != this.typeAdapterManager) {
			deserializer = this.typeAdapterManager.getDeserializer(json, type);
		}
		// 全局自定义反序列化
		if (null == deserializer) {
			deserializer = TypeAdapterManager.getInstance().getDeserializer(json, type);
		}
		final boolean ignoreError = ObjUtil.defaultIfNull(this.jsonConfig, JSONConfig::isIgnoreError, false);
		if (null == deserializer) {
			if (ignoreError) {
				return null;
			}
			throw new JSONException("No deserializer for type: " + type);
		}

		try {
			return (T) deserializer.deserialize(json, type);
		} catch (final Exception e) {
			if (ignoreError) {
				return null;
			}
			throw e;
		}
	}

	/**
	 * 将JSONObject转换为JSONArrayM<br>
	 * 在普通的Serializer中，JSONObject会被直接返回，如果想转为JSONArray，
	 *
	 * @param jsonObject JSONObject
	 * @return JSONArray
	 */
	public JSONArray mapFromJSONObject(final JSONObject jsonObject) {
		final JSONArray array = JSONUtil.ofArray(jsonConfig);
		for (final Map.Entry<String, JSON> entry : jsonObject) {
			array.add(entry.getValue());
		}
		return array;
	}

	/**
	 * 解析JSON字符串或XML字符串为JSON结构<br>
	 * 在普通的Serializer中，字符串会被作为普通字符串转为{@link JSONPrimitive},此处做区分
	 *
	 * @param source JSON字符串或XML字符串
	 * @return JSON对象
	 */
	public JSON map(final CharSequence source) {
		final String jsonStr = StrUtil.trim(source);
		if (StrUtil.startWith(jsonStr, '<')) {
			// 可能为XML
			final JSONObject jsonObject = JSONUtil.ofObj(jsonConfig);
			JSONXMLParser.of(ParseConfig.of(), this.predicate).parseJSONObject(jsonStr, jsonObject);
			return jsonObject;
		}

		return mapFromTokener(new JSONTokener(source));
	}

	/**
	 * 在需要的时候转换映射对象<br>
	 * 包装包括：
	 * <ul>
	 * <li>array or collection =》 JSONArray</li>
	 * <li>map =》 JSONObject</li>
	 * <li>standard property (Double, String, et al) =》 原对象</li>
	 * <li>来自于java包 =》 字符串</li>
	 * <li>其它 =》 尝试包装为JSONObject，否则返回{@code null}</li>
	 * </ul>
	 *
	 * @param obj 被映射的对象
	 * @return 映射后的值，null表示此值需被忽略
	 */
	public JSON map(Object obj) {
		if (null == obj) {
			return null;
		}

		if (obj instanceof Optional) {
			obj = ((Optional<?>) obj).orElse(null);
			if (null == obj) {
				return null;
			}
		} else if (obj instanceof Opt) {
			obj = ((Opt<?>) obj).getOrNull();
			if (null == obj) {
				return null;
			}
		}

		if (obj instanceof JSON) {
			return (JSON) obj;
		}

		final Class<?> clazz = obj.getClass();
		JSONSerializer<Object> serializer = null;
		// 自定义序列化
		if (null != this.typeAdapterManager) {
			serializer = this.typeAdapterManager.getSerializer(obj, clazz);
		}
		// 全局自定义序列化
		if (null == serializer) {
			serializer = TypeAdapterManager.getInstance().getSerializer(obj, clazz);
		}
		final boolean ignoreError = ObjUtil.defaultIfNull(this.jsonConfig, JSONConfig::isIgnoreError, false);
		if (null == serializer) {
			if (ignoreError) {
				return null;
			}
			throw new JSONException("No deserializer for type: " + obj.getClass());
		}

		try {
			return serializer.serialize(obj, new SimpleJSONContext(null, this.jsonConfig));
		} catch (final Exception e) {
			if (ignoreError) {
				return null;
			}
			throw e;
		}
	}

	/**
	 * 从{@link JSONTokener} 中读取JSON字符串，并转换为JSON
	 *
	 * @param tokener {@link JSONTokener}
	 * @return JSON
	 */
	private JSON mapFromTokener(final JSONTokener tokener) {
		return JSONParser.of(tokener, jsonConfig).setPredicate(this.predicate).parse();
	}
}