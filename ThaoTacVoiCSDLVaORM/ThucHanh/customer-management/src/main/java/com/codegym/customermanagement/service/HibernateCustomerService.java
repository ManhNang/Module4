package com.codegym.customermanagement.service;

import com.codegym.customermanagement.model.Customer;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.List;

@Service
public class HibernateCustomerService implements ICustomerService {
    private static SessionFactory sessionFactory;

    static {
        try {
            sessionFactory = new Configuration()
                    .configure("hibernate.conf.xml")
                    .buildSessionFactory();
        } catch (Throwable ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    @Override
    public List<Customer> findAll() {
        Session session = null;
        try {
            session = sessionFactory.openSession();
            String queryStr = "SELECT c FROM Customer AS c";
            TypedQuery<Customer> query = session.createQuery(queryStr, Customer.class);
            return query.getResultList();
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    @Override
    public Customer findById(int id) {
        Session session = null;
        try {
            session = sessionFactory.openSession();
            String queryStr = "SELECT c FROM Customer AS c WHERE c.id = :id";
            TypedQuery<Customer> query = session.createQuery(queryStr, Customer.class);
            query.setParameter("id", id);
            return query.getSingleResult();
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    @Override
    public void save(Customer customer) {
        Session session = null;
        Transaction transaction = null;
        try {
            session = sessionFactory.openSession();
            transaction = session.beginTransaction();
            if (customer.getId() != 0) {
                session.merge(customer);
            } else {
                session.persist(customer);
            }
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null)
                transaction.rollback();
            e.printStackTrace();
        } finally {
            if (session != null)
                session.close();
        }
    }

    @Override
    public void remove(int id) {
        Session session = null;
        Transaction transaction = null;
        try {
            session = sessionFactory.openSession();
            transaction = session.beginTransaction();
            Customer customer = session.get(Customer.class, id);
            if (customer != null) {
                session.remove(customer);
            }
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null)
                transaction.rollback();
            e.printStackTrace();
        } finally {
            if (session != null)
                session.close();
        }
    }
}