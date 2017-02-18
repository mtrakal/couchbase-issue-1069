package cz.mtrakal.cbl_issue_1069;

import android.util.Log;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Document;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryEnumerator;
import com.couchbase.lite.QueryRow;
import com.couchbase.lite.SavedRevision;
import com.couchbase.lite.TransactionalTask;
import com.couchbase.lite.UnsavedRevision;

import java.util.List;

/**
 * Created by muller on 04/11/2016.
 */
public class CouchBaseUtils {

    /**
     * Checks all the conflicts and resolved them by using the last revision
     */
    public static void checkAndResolveConflicts() {
        App.getApp().getDatabase().runInTransaction(
                new TransactionalTask() {
                    @Override
                    public boolean run() {
                        try {
                            Query allDocumentsQuery = App.getApp().getDatabase().createAllDocumentsQuery();
                            allDocumentsQuery.setAllDocsMode(Query.AllDocsMode.ONLY_CONFLICTS);
                            QueryEnumerator rows = allDocumentsQuery.run();
                            Log.i(App.TAG, rows.getCount() + " conflicts found");
                            while (rows.hasNext()) {
                                QueryRow queryRow = rows.next();
                                Document document = queryRow.getDocument();

                                // Delete the conflicting revisions to get rid of the conflict:
                                SavedRevision current = document.getCurrentRevision();
                                for (SavedRevision rev : document.getConflictingRevisions()) {
                                    Log.i(App.TAG, "Resolving conflict: " + rev);
                                    UnsavedRevision newRev = rev.createRevision();
                                    if (!rev.getId().equals(current.getId())) {
                                        newRev.setIsDeletion(true);
                                    }
                                    // saveAllowingConflict allows 'rev' to be updated even if it
                                    // is not the document's current revision.
                                    SavedRevision savedRevision = newRev.save(true);
                                    Log.i(App.TAG, "SavedRevision" + savedRevision);
                                }
                            }
                        } catch (CouchbaseLiteException e) {
                            return false;
                        } catch (IllegalStateException ex) {
                            Log.i(App.TAG, "error while resolving conflicts", ex);
                        }
                        return true;
                    }
                });
    }

    /**
     * Checks conflicts on particular document and resolved them by using the last revision.
     *
     * @param document The document to be checked for conflicts.
     */
    public static void checkAndResolveConflict(Document document) {
        try {
            resolveConflict(document);
        } catch (CouchbaseLiteException e) {
            Log.e(App.TAG, "Error while resolving conflicts");
        }
    }

    private static void resolveConflict(final Document doc) throws CouchbaseLiteException {
        final List<SavedRevision> conflicts = doc.getConflictingRevisions();
        if (conflicts.size() > 1) {
            // There is more than one current revision, thus a conflict!
            App.getApp().getDatabase().runInTransaction(new TransactionalTask() {
                @Override
                public boolean run() {
                    try {
                        // Delete the conflicting revisions to get rid of the conflict:
                        SavedRevision current = doc.getCurrentRevision();
                        for (SavedRevision rev : conflicts) {
                            Log.i(App.TAG, "Resolving conflict: " + rev);
                            UnsavedRevision newRev = rev.createRevision();
                            if (!rev.getId().equals(current.getId())) {
                                newRev.setIsDeletion(true);
                            }
                            // saveAllowingConflict allows 'rev' to be updated even if it
                            // is not the document's current revision.
                            SavedRevision savedRevision = newRev.save(true);
                            Log.i(App.TAG, "SavedRevision" + savedRevision);
                        }
                    } catch (CouchbaseLiteException e) {
                        return false;
                    }
                    return true;
                }
            });
        }
    }
}
