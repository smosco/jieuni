package com.jieuni.user;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByProviderAndProviderId(String provider, String providerId);

	// 한 사용자의 요청을 줄 세운다(제보 하루 한도를 세는 동안 같은 사용자의 다른 제보가 끼어들지 못하게).
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select u from User u where u.id = :id")
	Optional<User> findForUpdate(long id);

	@Query("select u.role from User u where u.id = :id")
	Optional<UserRole> findRoleById(Long id);

	@Modifying(clearAutomatically = true)
	@Query("update User u set u.role = com.jieuni.user.UserRole.USER where u.role = com.jieuni.user.UserRole.ADMIN")
	void demoteAllAdmins();

	@Modifying(clearAutomatically = true)
	@Query("update User u set u.role = com.jieuni.user.UserRole.ADMIN where lower(u.email) in :emails")
	void promoteByEmails(Collection<String> emails);

}
