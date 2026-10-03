package com.example.cv.cv;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

@Component
public class CvVersionDiffer {

    public List<CvVersionChange> diff(JsonNode oldSnapshot, JsonNode newSnapshot) {
        List<CvVersionChange> changes = new ArrayList<>();
        compare("", oldSnapshot, newSnapshot, changes);
        return List.copyOf(changes);
    }

    private void compare(String path, JsonNode oldValue, JsonNode newValue, List<CvVersionChange> changes) {
        if (oldValue == null || oldValue.isMissingNode()) {
            changes.add(new CvVersionChange(path, CvVersionChange.Type.ADDED, null, copy(newValue)));
            return;
        }
        if (newValue == null || newValue.isMissingNode()) {
            changes.add(new CvVersionChange(path, CvVersionChange.Type.REMOVED, copy(oldValue), null));
            return;
        }
        if (oldValue.equals(newValue)) {
            return;
        }
        if (oldValue.isObject() && newValue.isObject()) {
            Set<String> fields = new TreeSet<>();
            oldValue.fieldNames().forEachRemaining(fields::add);
            newValue.fieldNames().forEachRemaining(fields::add);
            for (String field : fields) {
                compare(path.isEmpty() ? field : path + "." + field,
                        oldValue.path(field), newValue.path(field), changes);
            }
            return;
        }
        if (oldValue.isArray() && newValue.isArray()) {
            int maxLength = Math.max(oldValue.size(), newValue.size());
            for (int index = 0; index < maxLength; index++) {
                compare(path + "[" + index + "]", oldValue.path(index), newValue.path(index), changes);
            }
            return;
        }
        changes.add(new CvVersionChange(path, CvVersionChange.Type.MODIFIED, copy(oldValue), copy(newValue)));
    }

    private JsonNode copy(JsonNode value) {
        return value == null || value.isMissingNode() ? null : value.deepCopy();
    }
}