package com.codegym.customermanagementjpa.repository;

import com.codegym.customermanagementjpa.model.Feedback;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public class FeedbackRepository implements IFeedbackRepository {

    @PersistenceContext
    private EntityManager em;

    @Override
    public List<Feedback> findAll() {
        TypedQuery<Feedback> query = em.createQuery("SELECT f FROM Feedback f ORDER BY f.id DESC", Feedback.class);
        return query.getResultList();
    }

    @Override
    public List<Feedback> findAllToday() {
        TypedQuery<Feedback> query = em.createQuery(
                "SELECT f FROM Feedback f WHERE f.date = :today ORDER BY f.id DESC",
                Feedback.class
        );
        query.setParameter("today", LocalDate.now());
        return query.getResultList();
    }

    @Override
    public Feedback findById(Long id) {
        return em.find(Feedback.class, id);
    }

    @Override
    public void save(Feedback feedback) {
        if (feedback.getId() == null) {
            em.persist(feedback);
        } else {
            em.merge(feedback);
        }
    }

    @Override
    public void incrementLike(Long id) {
        Feedback feedback = findById(id);
        if (feedback != null) {
            feedback.setLikes(feedback.getLikes() + 1);
            em.merge(feedback);
        }
    }
}
