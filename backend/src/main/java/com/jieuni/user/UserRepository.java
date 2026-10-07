package com.jieuni.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByProviderAndProviderId(String provider, String providerId);

	@Query("select u.role from User u where u.id = :id")
	Optional<UserRole> findRoleById(Long id);

	@Modifying(clearAutomatically = true)
	@Query("update User u set u.role = com.jieuni.user.UserRole.USER where u.role = com.jieuni.user.UserRole.ADMIN")
	void demoteAllAdmins();

	@Modifying(clearAutomatically = true)
	@Query("update User u set u.role = com.jieuni.user.UserRole.ADMIN where lower(u.email) in :emails")
	void promoteByEmails(Collection<String> emails);

}
