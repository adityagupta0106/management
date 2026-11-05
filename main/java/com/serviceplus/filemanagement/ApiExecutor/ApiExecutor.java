package com.serviceplus.filemanagement.ApiExecutor;

import java.util.Map;

import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

public interface ApiExecutor<T> {

	ResponseEntity<? extends Object> callExternalEndpoint(Class<?> returnObj,final HttpMethod httpMethod,
			Map<String,String> headers,Map<String,Object> params,String url,
			String bodyContent,final MediaType contentType);
}
