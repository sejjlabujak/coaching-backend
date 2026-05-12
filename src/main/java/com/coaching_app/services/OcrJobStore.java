package com.coaching_app.services;

import com.coaching_app.dto.OcrConfirmDTO;
import com.coaching_app.dto.OcrUploadResponseDTO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class OcrJobStore {

    private static final ConcurrentHashMap<String, OcrUploadResponseDTO> store
            = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, List<OcrConfirmDTO>> drillsStore
            = new ConcurrentHashMap<>();

    public static void put(String id, OcrUploadResponseDTO dto) { store.put(id, dto); }
    public static OcrUploadResponseDTO get(String id) { return store.get(id); }

    public static void putDrills(String id, List<OcrConfirmDTO> drills) { drillsStore.put(id, drills); }
    public static List<OcrConfirmDTO> getDrills(String id) { return drillsStore.get(id); }
}