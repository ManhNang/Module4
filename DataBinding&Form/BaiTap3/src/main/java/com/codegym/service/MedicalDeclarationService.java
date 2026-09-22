package com.codegym.service;

import com.codegym.model.MedicalDeclaration;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class MedicalDeclarationService implements IMedicalDeclarationService {
    private final Map<Integer, MedicalDeclaration> declarationMap = new ConcurrentHashMap<>();
    private final AtomicInteger idGenerator = new AtomicInteger(1);

    public MedicalDeclarationService() {
        initSampleData();
    }

    private void initSampleData() {
        int id1 = idGenerator.getAndIncrement();
        MedicalDeclaration sample1 = new MedicalDeclaration(
                id1,
                "NGUYỄN VĂN AN",
                1990,
                "Nam",
                "Việt Nam",
                "001090012345",
                "Tàu bay",
                "VN-123",
                "12A",
                "2026-09-15",
                "2026-09-15",
                "Đà Nẵng, TP Hồ Chí Minh",
                "Hà Nội",
                "Cầu Giấy",
                "Dịch Vọng Hậu",
                "Số 12 Duy Tân",
                "0987654321",
                "an.nguyen@example.com",
                Arrays.asList("Ho", "Sốt"),
                Collections.singletonList("Tiếp xúc gần với người nghi ngờ mắc bệnh"),
                null,
                null
        );
        declarationMap.put(id1, sample1);

        int id2 = idGenerator.getAndIncrement();
        MedicalDeclaration sample2 = new MedicalDeclaration(
                id2,
                "TRẦN THỊ HỒNG",
                1995,
                "Nữ",
                "Việt Nam",
                "079195009876",
                "Ô tô",
                "29B-888.88",
                "05",
                "2026-09-18",
                "2026-09-18",
                "Quảng Ninh",
                "TP Hồ Chí Minh",
                "Quận 1",
                "Bến Nghé",
                "Số 45 Lê Lợi",
                "0912345678",
                "hong.tran@example.com",
                Collections.emptyList(),
                Collections.emptyList(),
                null,
                null
        );
        declarationMap.put(id2, sample2);
    }

    @Override
    public List<MedicalDeclaration> findAll() {
        List<MedicalDeclaration> list = new ArrayList<>(declarationMap.values());
        list.sort(Comparator.comparingInt(MedicalDeclaration::getId).reversed());
        return list;
    }

    @Override
    public MedicalDeclaration findById(int id) {
        return declarationMap.get(id);
    }

    @Override
    public void save(MedicalDeclaration declaration) {
        if (declaration.getId() == 0) {
            declaration.setId(idGenerator.getAndIncrement());
        }
        declarationMap.put(declaration.getId(), declaration);
    }

    @Override
    public void update(int id, MedicalDeclaration declaration) {
        declaration.setId(id);
        declarationMap.put(id, declaration);
    }

    @Override
    public void delete(int id) {
        declarationMap.remove(id);
    }

    @Override
    public int getNextId() {
        return idGenerator.getAndIncrement();
    }
}
