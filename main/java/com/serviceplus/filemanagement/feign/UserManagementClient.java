package com.serviceplus.filemanagement.feign;

import com.serviceplus.filemanagement.dto.DSCVerificationDetails;
import com.serviceplus.filemanagement.dto.DSCVerificationRequest;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "USERMANAGEMENT", path = "/usermgmt", url = "${usermanagement.service.url}")
public interface UserManagementClient {

	@PostMapping(value = "/b/dsc/verification-details", consumes = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<DSCVerificationDetails> getDSCVerificationDetails(@RequestBody DSCVerificationRequest request);
}
