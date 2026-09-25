package com.codegym.image.service;

import com.codegym.image.model.Feedback;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

@Service
public class FeedbackService implements IFeedbackService {

    private final SessionFactory sessionFactory;

    @Autowired
    public FeedbackService(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public List<Feedback> findAllToday() {
        try (Session session = sessionFactory.openSession()) {
            String hql = "FROM Feedback f WHERE f.date = :today ORDER BY f.id DESC";
            return session.createQuery(hql, Feedback.class)
                    .setParameter("today", LocalDate.now())
                    .getResultList();
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    @Override
    public Feedback findById(Long id) {
        try (Session session = sessionFactory.openSession()) {
            return session.get(Feedback.class, id);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void save(Feedback feedback) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            if (feedback.getDate() == null) {
                feedback.setDate(LocalDate.now());
            }
            session.persist(feedback);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            e.printStackTrace();
        }
    }

    @Override
    public void like(Long id) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            Feedback feedback = session.get(Feedback.class, id);
            if (feedback != null) {
                feedback.setLikes(feedback.getLikes() + 1);
                session.merge(feedback);
            }
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            e.printStackTrace();
        }
    }
}
