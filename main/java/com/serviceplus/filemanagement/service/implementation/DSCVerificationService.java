package com.serviceplus.filemanagement.service.implementation;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.serviceplus.filemanagement.ApiExecutor.ApiExecutor;
import com.serviceplus.filemanagement.ExceptionHandler.SPRuntimeError;

@Service
public class DSCVerificationService {

	private final ApiExecutor apiExecutor;
	
	@Value("${dsc.api.server.ip}")
    private String dscApiServerIp;

	public DSCVerificationService(ApiExecutor apiExecutor) {
		this.apiExecutor = apiExecutor;
	}

	public byte[] verifySignedDocument(String signedBase64, String key, String certificate, String documentType) {

		try {

			String url = getVerificationUrl(documentType);

			Map<String, Object> body = new HashMap<>();

			body.put("action", "DECRYPT_VERIFY_WITH_CERT");

			body.put("en_sig", signedBase64);

			body.put("ek", key);

			body.put("certificate", certificate);

			ResponseEntity<?> apiResponse = apiExecutor.callExternalEndpoint(String.class, HttpMethod.POST,
					new HashMap<>(), Collections.emptyMap(), url, new JSONObject(body).toString(),
					MediaType.APPLICATION_JSON);

			if (apiResponse == null || apiResponse.getBody() == null) {

				throw new SPRuntimeError("Invalid response from DSC verification service", HttpStatus.BAD_REQUEST);
			}

			JSONObject response = new JSONObject(apiResponse.getBody().toString());

			String statusCode = response.get("status_cd").toString();

			if ("1".equals(statusCode)) {

				String data = response.getString("data");

				byte[] decodedData = Base64.getDecoder().decode(data);

				JSONObject dataObject = new JSONObject(new String(decodedData, StandardCharsets.UTF_8));

				if (dataObject.isNull("sig")) {

					throw new SPRuntimeError("Signature not found after verification", HttpStatus.BAD_REQUEST);
				}

				return Base64.getDecoder().decode(dataObject.getString("sig"));
			}

			if ("0".equals(statusCode)) {

				String errorCode = response.has("error_cd") ? response.getString("error_cd") : null;

				throw new SPRuntimeError(getVerificationError(errorCode), HttpStatus.BAD_REQUEST);
			}

			throw new SPRuntimeError("Invalid response from DSC verification service", HttpStatus.BAD_REQUEST);

		} catch (SPRuntimeError ex) {

			throw ex;

		} catch (Exception ex) {

			ex.printStackTrace();

			throw new SPRuntimeError("Something went wrong while verifying DSC", HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	private String getVerificationUrl(String documentType) {

		if ("XML".equalsIgnoreCase(documentType)) {

			return dscApiServerIp + "/xmlsignature";
		}

		return dscApiServerIp + "/pdfsignature";
	}

	private String getVerificationError(String errorCode) {

		if (errorCode == null || errorCode.isBlank()) {

			return "Verification Failed";
		}

		return switch (errorCode) {

		case "1001" -> "Decryption Failed";

		case "1002" -> "Signature Verification failed";

		case "1003" -> "Certificate Verification failed";

		case "1004" -> "Invalid signing time";

		case "1005" -> "Certificate is Expired";

		case "1006" -> "Certificate Mismatch";

		case "9000" -> "Invalid request format";

		case "2005" -> "Signature Verification error";

		default -> "Verification Failed";
		};
	}
}
