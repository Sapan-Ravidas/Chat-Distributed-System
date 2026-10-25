package com.sapan.chat.connection.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sapan.chat.connection.model.ConnectionEntity;
import com.sapan.chat.connection.model.ConnectionStatus;

public interface ConnectionRepository extends JpaRepository<ConnectionEntity, UUID> {

	Optional<ConnectionEntity> findByPairKey(String pairKey);

	List<ConnectionEntity> findByRecipientUsernameAndStatusOrderByCreatedAtDesc(
			String recipientUsername, ConnectionStatus status);

	List<ConnectionEntity> findByRequesterUsernameAndStatusOrderByCreatedAtDesc(
			String requesterUsername, ConnectionStatus status);

	@Query("""
			select connection
			from ConnectionEntity connection
			where connection.status = :status
			  and (connection.requesterUsername = :username or connection.recipientUsername = :username)
			order by connection.updatedAt desc
			""")
	List<ConnectionEntity> findAllForUserWithStatus(
			@Param("username") String username,
			@Param("status") ConnectionStatus status);
}
