package com.easybytes.account.controller;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.easybytes.account.config.AccountsServiceConfig;
import com.easybytes.account.model.Accounts;
import com.easybytes.account.model.Cards;
import com.easybytes.account.model.Customer;
import com.easybytes.account.model.CustomerDetails;
import com.easybytes.account.model.Loans;
import com.easybytes.account.model.Properties;
import com.easybytes.account.repository.AccountsRespository;
import com.easybytes.account.service.client.CardsFeignClient;
import com.easybytes.account.service.client.LoansFeignClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.annotation.Timed;

@RestController
public class AccountsController {

	private static final Logger logger = LoggerFactory.getLogger(AccountsController.class);
	
	
	@Autowired
	private AccountsRespository accountsRepositry;
	
	@Autowired
	AccountsServiceConfig accountsConfig;
	
	@Autowired
	CardsFeignClient cardsFeignClient;
	
	@Autowired
	LoansFeignClient loansFeignClient;
	
	
	
	@PostMapping("/myAccount")
	@Timed(value="getAccountDetails.time",description="Time taken to return Account details")
	public Accounts getAccountsDetails(@RequestBody Customer customer)
	{
		Accounts accounts=accountsRepositry.findByCustomerId(customer.getCustomerId());
		if(accounts!=null)
		{
			return accounts;
		}
		else
		{
			return null;
		}
	}
	
	
	@GetMapping("/account/properties")
	public String getPropertyDetails() throws JsonProcessingException {
		ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
		Properties properties = new Properties(accountsConfig.getMsg(), accountsConfig.getBuildVersion(),
				accountsConfig.getMailDetails(), accountsConfig.getActiveBranches());
		String jsonStr = ow.writeValueAsString(properties);
		return jsonStr;
	}
	
	@PostMapping("/myCustomerDetails")
	//@CircuitBreaker(name="detailsForCustomerSupportApp",fallbackMethod="myCustomerDetailsFallback")
	@Retry(name="retryForCustomerDetails",fallbackMethod="myCustomerDetailsFallback")
	public CustomerDetails myCustomerDetails(@RequestHeader("eazybank-correlation-id") String correlationid,@RequestBody Customer customer)
	{
		logger.info("myCustomerDetails() method started");

		CustomerDetails customerDetails = new CustomerDetails();

		Accounts accounts=accountsRepositry.findByCustomerId(customer.getCustomerId());
		List<Cards> cards=cardsFeignClient.getCardDetails(correlationid,customer);
		List<Loans> loans=loansFeignClient.getLoansDetails(correlationid,customer);
		
		customerDetails.setAccounts(accounts);
		customerDetails.setCards(cards);
		customerDetails.setLoans(loans);
		
		logger.info("myCustomerDetails() method ended");

		return customerDetails;
	}
	
	private CustomerDetails myCustomerDetailsFallback(@RequestHeader("eazybank-correlation-id") String correlationid,Customer customer,Throwable th)
	{
		CustomerDetails customerDetails = new CustomerDetails();
		
		Accounts accounts=accountsRepositry.findByCustomerId(customer.getCustomerId());
		List<Loans> loans=loansFeignClient.getLoansDetails(correlationid,customer);
		
		customerDetails.setAccounts(accounts);
		customerDetails.setLoans(loans);
		
		return customerDetails;
	}
	
	@GetMapping("/sayHello")
	@RateLimiter(name="sayHello",fallbackMethod="sayHelloFallback")
	public String sayHello()
	{
		
		Optional<String> podName=Optional.ofNullable(System.getenv("HOSTNAME"));
		return "Hello welcome to eazybank easybank kubernetes cluster from "+podName.get();
	}
	
	private String sayHelloFallback(Throwable th)
	{
		return "Hi this is eazybank";
	}
	
}

