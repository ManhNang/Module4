package com.codegym.musicstreaming.service;

import com.codegym.musicstreaming.model.Song;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class SongServiceImpl implements ISongService {

    @Autowired
    private SessionFactory sessionFactory;

    @Override
    public List<Song> findAll() {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery("FROM Song ORDER BY id DESC", Song.class).getResultList();
        } catch (HibernateException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    @Override
    public Song findById(Long id) {
        try (Session session = sessionFactory.openSession()) {
            return session.get(Song.class, id);
        } catch (HibernateException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void save(Song song) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            session.persist(song);
            transaction.commit();
        } catch (HibernateException e) {
            if (transaction != null) {
                transaction.rollback();
            }
            e.printStackTrace();
        }
    }

    @Override
    public void update(Song song) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            session.merge(song);
            transaction.commit();
        } catch (HibernateException e) {
            if (transaction != null) {
                transaction.rollback();
            }
            e.printStackTrace();
        }
    }

    @Override
    public void remove(Long id) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            Song song = session.get(Song.class, id);
            if (song != null) {
                session.remove(song);
            }
            transaction.commit();
        } catch (HibernateException e) {
            if (transaction != null) {
                transaction.rollback();
            }
            e.printStackTrace();
        }
    }

    @Override
    public List<Song> searchByName(String keyword) {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery(
                    "FROM Song WHERE LOWER(name) LIKE LOWER(:keyword) OR LOWER(artist) LIKE LOWER(:keyword) ORDER BY id DESC",
                    Song.class
            ).setParameter("keyword", "%" + keyword + "%").getResultList();
        } catch (HibernateException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }
}
