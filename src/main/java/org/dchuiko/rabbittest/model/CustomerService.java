package org.dchuiko.rabbittest.model;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class CustomerService {
    private final CustomerRepository repository;

    @Autowired
    public CustomerService(CustomerRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void createCustomers(List<Customer> customers) {
        customers.forEach(repository::save);
    }

    @Transactional(readOnly = true)
    public List<Customer> findAll() {
        List<Customer> result = new ArrayList<>();
        repository.findAll().forEach(result::add);
        return result;
    }

    @Transactional(readOnly = true)
    public long count() {
        return repository.count();
    }
}
