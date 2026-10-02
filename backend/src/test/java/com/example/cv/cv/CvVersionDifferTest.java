package com.example.cv.cv;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CvVersionDifferTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CvVersionDiffer differ = new CvVersionDiffer();

    @Test
    void reportsAddedRemovedAndModifiedFieldsIncludingArrayEntries() throws Exception {
        var before = objectMapper.readTree("""
                {"name":"Old","description":"remove me","content":{"summary":"before","skills":["Java"]}}
                """);
        var after = objectMapper.readTree("""
                {"name":"New","content":{"summary":"after","skills":["Java","SQL"]},"language":"en"}
                """);

        List<CvVersionChange> changes = differ.diff(before, after);

        assertThat(changes).extracting(CvVersionChange::path)
                .containsExactly("content.skills[1]", "content.summary", "description", "language", "name");
        assertThat(changes).extracting(CvVersionChange::type)
                .containsExactly(CvVersionChange.Type.ADDED, CvVersionChange.Type.MODIFIED,
                        CvVersionChange.Type.REMOVED, CvVersionChange.Type.ADDED, CvVersionChange.Type.MODIFIED);
    }

    @Test
    void returnsNoChangesForEqualSnapshots() throws Exception {
        var snapshot = objectMapper.readTree("{\"name\":\"Resume\"}");

        assertThat(differ.diff(snapshot, snapshot.deepCopy())).isEmpty();
    }
}