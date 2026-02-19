package com.nirmalks.audit_service.audit.web;

import com.nirmalks.audit_service.audit.entity.AuditLog;
import com.nirmalks.audit_service.audit.repository.AuditLogRepository;
import com.nirmalks.audit_service.audit.repository.AuditLogSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
public class AuditController {

	private final AuditLogRepository repository;

	@GetMapping
	public Page<AuditLog> search(@RequestParam(required = false) String principal,
			@RequestParam(required = false) String action, @RequestParam(required = false) String resource,
			@RequestParam(required = false) String status, @RequestParam(required = false) String traceId,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
			Pageable pageable) {
		// Enforce default ordering by occurredAt DESC if not specified
		// But Pageable usually comes from frontend.
		// We can check if pageable.getSort() is unsorted.
		return repository.findAll(AuditLogSpecification.filter(principal, action, resource, status, traceId, from, to),
				pageable);
	}

}
