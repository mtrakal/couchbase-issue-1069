package cz.mtrakal.cbl_issue_1069;

import android.app.Application;
import android.os.Handler;
import android.widget.Toast;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.DatabaseOptions;
import com.couchbase.lite.Document;
import com.couchbase.lite.DocumentChange;
import com.couchbase.lite.Manager;
import com.couchbase.lite.android.AndroidContext;
import com.couchbase.lite.auth.Authenticator;
import com.couchbase.lite.auth.AuthenticatorFactory;
import com.couchbase.lite.replicator.Replication;
import com.couchbase.lite.util.Log;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by mtrakal on 18.02.2017.
 */

public class App extends Application implements Replication.ChangeListener {
    public static final String DATABASE_NAME = "bb-365fd8f9-1c60-49e0-b77b-bf4cf181007e";
    public static final String TAG = "App";
    private static final String COUCH_DB_URL = "http://10.0.0.3:5984/" + DATABASE_NAME;
    private static final String ENCRYPTION_USER = "admin";
    private static final String ENCRYPTION_KEY = "admin";

    private static final String STORAGE_TYPE = Manager.FORESTDB_STORAGE;

    private static final boolean LOGGING_ENABLED = true;
    private static App mApp;

    private Manager mManager;
    private Database mDatabase;
    private Replication mPull;
    private Replication mPush;
    private Throwable mReplError;
    private String mCurrentUserId;

    @Override
    public void onCreate() {
        super.onCreate();
        enableLogging();
        mApp = this;

        setDatabase(getUserDatabase(DATABASE_NAME));
        setChangeListener();
    }

    private void setChangeListener() {
        mDatabase.addChangeListener(getDatabaseChangeListener());
    }

    private void enableLogging() {
        if (LOGGING_ENABLED) {
            Manager.enableLogging(TAG, Log.VERBOSE);
            Manager.enableLogging(Log.TAG, Log.VERBOSE);
            Manager.enableLogging(Log.TAG_SYNC_ASYNC_TASK, Log.VERBOSE);
            Manager.enableLogging(Log.TAG_SYNC, Log.VERBOSE);
            Manager.enableLogging(Log.TAG_QUERY, Log.VERBOSE);
            Manager.enableLogging(Log.TAG_VIEW, Log.VERBOSE);
            Manager.enableLogging(Log.TAG_DATABASE, Log.VERBOSE);
        }
    }

    private Manager getManager() {
        if (mManager == null) {
            try {
                AndroidContext context = new AndroidContext(getApplicationContext());
                mManager = new Manager(context, Manager.DEFAULT_OPTIONS);
            } catch (Exception e) {
                Log.e(TAG, "Cannot create Manager object", e);
            }
        }
        return mManager;
    }

    public Database getDatabase() {
        return mDatabase;
    }

    private void setDatabase(Database database) {
        this.mDatabase = database;
    }

    private Database getUserDatabase(String name) {
        try {
            String dbName = name;
            DatabaseOptions options = new DatabaseOptions();
            options.setCreate(true);
            options.setStorageType(STORAGE_TYPE);
//            options.setEncryptionKey(ENCRYPTION_ENABLED ? ENCRYPTION_KEY : null);
            return getManager().openDatabase(dbName, options);
        } catch (CouchbaseLiteException e) {
            Log.e(TAG, "Cannot create database for name: " + name, e);
        }
        return null;
    }

    private URL getSyncUrl() {
        URL url = null;
        try {
            url = new URL(COUCH_DB_URL);
        } catch (MalformedURLException e) {
            Log.e(TAG, "Invalid sync url", e);
        }
        return url;
    }

    private Authenticator getAuthenticator() {
        return AuthenticatorFactory.createBasicAuthenticator(ENCRYPTION_USER, ENCRYPTION_KEY);
    }

    public void startReplication() {
        if (mPull == null) {
            mPull = mDatabase.createPullReplication(getSyncUrl());
            mPull.setContinuous(false);
            mPull.setAuthenticator(getAuthenticator());
            mPull.addChangeListener(this);
        }
        mPull.start();
    }

    @Override
    public void changed(Replication.ChangeEvent event) {
        Throwable error = null;
        if (mPull != null) {
            if (error == null)
                error = mPull.getLastError();
        }

        if (error != mReplError) {
            mReplError = error;
            if (mReplError != null)
                showErrorMessage(mReplError.getMessage(), null);
        }
    }


    public Database.ChangeListener getDatabaseChangeListener() {
        return new Database.ChangeListener() {
            @Override
            public void changed(Database.ChangeEvent event) {
                for (DocumentChange documentChange : event.getChanges()) {
                    if (!documentChange.isCurrentRevision()) {
                        continue;
                    }
                    String documentId = documentChange.getDocumentId();

                    // first resolve conflicts
                    if (documentChange.isConflict()) {
                        Document document = getDatabase().getDocument(documentId);
                        CouchBaseUtils.checkAndResolveConflict(document);
                    }
                }
            }
        };
    }

    public void showErrorMessage(final String errorMessage, final Throwable throwable) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                android.util.Log.e(TAG, errorMessage, throwable);
                String msg = String.format("%s: %s",
                        errorMessage, throwable != null ? throwable : "");
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void runOnUiThread(Runnable runnable) {
        Handler mainHandler = new Handler(getApplicationContext().getMainLooper());
        mainHandler.post(runnable);
    }

    public static App getApp() {
        return mApp;
    }
}
