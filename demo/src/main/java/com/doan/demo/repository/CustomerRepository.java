package com.doan.demo.repository;

import com.doan.demo.model.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByPhoneNumber(String phone);

    List<Customer> findByFullNameContainingIgnoreCase(String name);

    List<Customer> findByPhoneNumberContaining(String phone);

    @Query("SELECT c FROM Customer c WHERE " +
            "LOWER(c.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "c.phoneNumber LIKE CONCAT('%', :keyword, '%')")
    List<Customer> searchByKeyword(@Param("keyword") String keyword);

    @Query("SELECT c FROM Customer c WHERE " +
            "LOWER(c.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "c.phoneNumber LIKE CONCAT('%', :keyword, '%')")
    Page<Customer> searchByKeywordPaged(@Param("keyword") String keyword, Pageable pageable);

    Page<Customer> findAll(Pageable pageable);

    @Query("SELECT c.tier, COUNT(c) FROM Customer c GROUP BY c.tier")
    List<Object[]> countByTier();

    /** Broadcast FCM: chỉ lấy fcm_token thay vì load toàn bộ Customer object */
    @Query("SELECT c.fcmToken FROM Customer c WHERE c.fcmToken IS NOT NULL AND c.fcmToken <> ''")
    List<String> findAllFcmTokens();
}