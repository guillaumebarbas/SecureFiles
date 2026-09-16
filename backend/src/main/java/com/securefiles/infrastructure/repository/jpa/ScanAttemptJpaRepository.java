package com.securefiles.infrastructure.repository.jpa;

import com.securefiles.infrastructure.entity.ScanAttemptEntity;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ScanAttemptJpaRepository extends JpaRepository<ScanAttemptEntity, UUID> {

		@Query("""
						select scanAttempt
							from ScanAttemptEntity scanAttempt
						 where scanAttempt.fileId in :fileIds
							 and scanAttempt.failureCode is not null
							 and scanAttempt.failureCode <> 'SCAN_ATTEMPTS_EXHAUSTED'
							 and scanAttempt.attemptNumber = (
										select max(latestAttempt.attemptNumber)
											from ScanAttemptEntity latestAttempt
										 where latestAttempt.fileId = scanAttempt.fileId
											 and latestAttempt.failureCode is not null
											 and latestAttempt.failureCode <> 'SCAN_ATTEMPTS_EXHAUSTED'
							 )
						""")
		List<ScanAttemptEntity> findLatestPreciseFailuresByFileIds(@Param("fileIds") Set<UUID> fileIds);
}
