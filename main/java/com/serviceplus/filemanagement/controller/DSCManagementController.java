package com.serviceplus.filemanagement.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.serviceplus.filemanagement.ExceptionHandler.SPRuntimeError;
import com.serviceplus.filemanagement.dto.DSCSignRequest;
import com.serviceplus.filemanagement.dto.DSCSignResponse;
import com.serviceplus.filemanagement.service.implementation.DSCManagementService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/a")
public class DSCManagementController {

	private final DSCManagementService dscManagementService;

	public DSCManagementController(DSCManagementService dscManagementService) {

		this.dscManagementService = dscManagementService;
	}

	@PostMapping("/dsc/sign")
	public ResponseEntity<DSCSignResponse> signDocument(HttpServletRequest httpRequest, @RequestBody DSCSignRequest request) {
		try {
			return ResponseEntity.ok(dscManagementService.signDocument(httpRequest,request));
		} catch (Exception ex) {
			if (ex instanceof SPRuntimeError) {
				throw (SPRuntimeError) ex;
			}
			ex.printStackTrace();

			throw new SPRuntimeError("Something went wrong", HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
}
