package com.noqapp.mobile.service;

import com.noqapp.medical.service.MedicalRecordService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * hitender
 * 2018-11-29 10:54
 */
@Service
public class MedicalRecordMobileService {

    private MedicalRecordService medicalRecordService;

    @Autowired
    public MedicalRecordMobileService(MedicalRecordService medicalRecordService) {
        this.medicalRecordService = medicalRecordService;
    }

    public String findAllFollowUp(String codeQR) {
        return medicalRecordService.findAllFollowUp(codeQR);
    }
}
