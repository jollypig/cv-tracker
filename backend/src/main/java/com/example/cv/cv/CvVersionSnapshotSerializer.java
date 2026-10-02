package com.example.cv.cv;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class CvVersionSnapshotSerializer {

    private final ObjectMapper objectMapper;

    public CvVersionSnapshotSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public JsonNode serialize(Cv cv, CvContent content) {
        return objectMapper.valueToTree(new CvVersionSnapshot(cv.getTemplateId(), cv.getName(),
                cv.getDescription(), cv.getLanguage(), cv.getStatus(), content));
    }

    public CvVersionSnapshot deserialize(JsonNode snapshot) {
        return objectMapper.convertValue(snapshot, CvVersionSnapshot.class);
    }
}