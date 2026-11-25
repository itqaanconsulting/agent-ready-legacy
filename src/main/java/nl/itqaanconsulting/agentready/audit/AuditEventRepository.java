package nl.itqaanconsulting.agentready.audit;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {

    List<AuditEvent> findByAgentOrderByIdDesc(String agent, Pageable page);

    List<AuditEvent> findAllByOrderByIdDesc(Pageable page);
}
