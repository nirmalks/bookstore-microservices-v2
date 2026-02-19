package com.nirmalks.audit_service.audit.repository;

import com.nirmalks.audit_service.audit.entity.AuditLog;
import com.nirmalks.audit_service.audit.entity.AuditLogId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AuditLogRepository extends JpaRepository<AuditLog, AuditLogId>, JpaSpecificationExecutor<AuditLog> {

}
