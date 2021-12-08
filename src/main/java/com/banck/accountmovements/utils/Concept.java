/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.banck.accountmovements.utils;

/**
 *
 * @author jonavcar
 */
public enum Concept {
    RETIRO("RETIRO") {
        @Override
        public boolean equals(String movementType) {
            return value.equals(movementType);
        }
    },
    DEPOSITO("DEPOSITO") {
        @Override
        public boolean equals(String movementType) {
            return value.equals(movementType);
        }
    },
    TRANSFERENCIA("TRANSFERENCIA") {
        @Override
        public boolean equals(String movementType) {
            return value.equals(movementType);
        }
    },
    AUTOMATICO("AUTOMATICO") {
        @Override
        public boolean equals(String movementType) {
            return value.equals(movementType);
        }
    };

    public final String value;

    public boolean equals(String movementType) {
        return value.equals(movementType);
    }

    private Concept(String value) {
        this.value = value;
    }
}
