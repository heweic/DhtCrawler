package org.my.pro.dhtcrawler.util;

import org.apache.commons.lang3.StringUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class GsonUtils {

	public static Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
	
	public static final String empty_Object = "{}";

	/**
	 * json 转 pojo
	 */
	public static <T> T getObject(String json, Class<T> t) {
		return GSON.fromJson(json, t);
	}

	/**
	 * pojo 转 json
	 */
	public static String toJsonString(Object obj) {
		if (null == obj) {
			return empty_Object;
		}
		if (obj instanceof String) {
			return (String) obj;
		}
		return GSON.toJson(obj, obj.getClass());
	}

	/** 
	 * 
	 * */
	public static JsonObject getJsonObject(String json) {
		if (StringUtils.isEmpty(json)) {
			return null;
		}
		JsonObject jsonObject = new JsonParser().parse(json).getAsJsonObject();
		return jsonObject;
	}

}
