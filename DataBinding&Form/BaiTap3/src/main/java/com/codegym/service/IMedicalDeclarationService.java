package com.codegym.service;

import com.codegym.model.MedicalDeclaration;
import java.util.List;

public interface IMedicalDeclarationService {
    List<MedicalDeclaration> findAll();

    MedicalDeclaration findById(int id);

    void save(MedicalDeclaration declaration);

    void update(int id, MedicalDeclaration declaration);

    void delete(int id);

    int getNextId();
}
