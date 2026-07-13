package com.chalchitraghar.modules.tickets.service;

public interface QrImageService {
    byte[] generatePng(String payload);
}
