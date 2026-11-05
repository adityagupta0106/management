package com.serviceplus.filemanagement.ApiExecutor;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class SynchronousApiExecutor<T> implements ApiExecutor<T> {

	@Autowired
	private RestTemplate restTemplate;


	@Override
	public ResponseEntity<?> callExternalEndpoint(Class<?> returnObj, HttpMethod httpMethod,
			Map<String, String> headers, Map<String, Object> params, String url, String bodyContent,
			MediaType contentType) {

		if (headers == null) {
			headers = new HashMap<>();
		}

		HttpHeaders headerParams = new HttpHeaders();
		headers.forEach(headerParams::add);
		headerParams.setContentType(contentType);

		HttpEntity<Object> requestEntity = new HttpEntity<>(bodyContent, headerParams);
		return restTemplate.exchange(url, httpMethod, requestEntity, returnObj, params);

	}

}
