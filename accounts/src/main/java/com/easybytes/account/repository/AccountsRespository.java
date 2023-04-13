package com.easybytes.account.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.easybytes.account.model.Accounts;

@Repository
public interface AccountsRespository extends CrudRepository<Accounts, Long> {

	Accounts findByCustomerId(int customerId);
}
