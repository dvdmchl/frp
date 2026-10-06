package org.dreamabout.sw.frp.be.module.accounting.connector;

import java.util.List;

/**
 * One page of records with the cursor of the next page.
 *
 * @param nextCursor opaque cursor to pass to the next {@code fetchRecords} call, {@code null} on the last page
 */
public record RecordPage(List<ExternalRecord> records, String nextCursor) {

    public RecordPage {
        records = List.copyOf(records);
    }

    public static RecordPage of(List<ExternalRecord> records, String nextCursor) {
        return new RecordPage(records, nextCursor);
    }

    public static RecordPage last(List<ExternalRecord> records) {
        return new RecordPage(records, null);
    }

    public boolean isLast() {
        return nextCursor == null;
    }
}
