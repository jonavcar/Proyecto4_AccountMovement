/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.banck.accountmovements.utils;

/**
 *
 * @author jonavcar
 */
public enum Modality {
    // VENTANILLA
    VENTANILLA("VENTANILLA") {
        @Override
        public boolean equals(String customerType) {
            return value.equals(customerType);
        }
    },
    // POS
    POS("POS") {
        @Override
        public boolean equals(String customerType) {
            return value.equals(customerType);
        }
    },
    // CAJERO-AUTOMATICO
    CAJERO("CAJERO") {
        @Override
        public boolean equals(String customerType) {
            return value.equals(customerType);
        }
    },
    // EFECTIVO-MOVIL
    EFECTIVO_MOVIL("EFECTIVO-MOVIL") {
        @Override
        public boolean equals(String customerType) {
            return value.equals(customerType);
        }
    },
    //BANCA-MOVIL
    BANCA_MOVIL("BANCA-MOVIL") {
        @Override
        public boolean equals(String customerType) {
            return value.equals(customerType);
        }
    };

    public final String value;

    public boolean equals(String customerType) {
        return value.equals(customerType);
    }

    private Modality(String value) {
        this.value = value;
    }
}
