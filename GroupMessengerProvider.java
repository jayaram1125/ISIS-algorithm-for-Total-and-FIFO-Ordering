package edu.buffalo.cse.cse486586.groupmessenger2;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.util.Log;

/**
 * GroupMessengerProvider is a key-value table. Once again, please note that we do not implement
 * full support for SQL as a usual ContentProvider does. We re-purpose ContentProvider's interface
 * to use it as a key-value table.
 *
 * Please read:
 *
 * http://developer.android.com/guide/topics/providers/content-providers.html
 * http://developer.android.com/reference/android/content/ContentProvider.html
 *
 * before you start to get yourself familiarized with ContentProvider.
 *
 * There are two methods you need to implement---insert() and query(). Others are optional and
 * will not be tested.
 *
 * @author stevko
 *
 */
public class GroupMessengerProvider extends ContentProvider {



    // Contacts Table Columns names
    private static final String KEY = "key";
    private static final String VALUE = "value";
    private static final String TABLENAME = "MyTable";
    private Databasehelper m_helper;


    public class Databasehelper extends SQLiteOpenHelper {



        public Databasehelper(Context context)
        {
            super(context, "MyDatabase", null, 2);
            Log.e("db:","Databasehandler Constructor");

        }


        // Creating Tables
        @Override
        public void onCreate(SQLiteDatabase db) {
            Log.v("db:","Database Create");
            String QUERY = "CREATE TABLE " + TABLENAME + "("
                    + KEY + " TEXT PRIMARY KEY,"
                    + VALUE + " TEXT" + ")";
            db.execSQL(QUERY);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
        {
            // Drop older table if existed
            db.execSQL("DROP TABLE IF EXISTS " + "Table");
            // Create tables again
            onCreate(db);
        }

        public void Insert(ContentValues values)
        {
            Log.v("content insert","enter");
            String key = values.getAsString("key");
            String Msg = values.getAsString("value");
            String QUERY = "INSERT OR REPLACE INTO MyTable (key,value) VALUES(\""+key+"\",\""+Msg+"\")";
            Log.v("content insert",QUERY);
            getWritableDatabase().execSQL(QUERY);
        }

        public Cursor Query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                            String sortOrder)
        {
            SQLiteQueryBuilder qbuild = new SQLiteQueryBuilder();
            qbuild.setTables(TABLENAME);
            Log.v("Databasehelper","Query");
            String Query = "SELECT * from MyTable WHERE key =" +"\""+ selection+"\"";
            Cursor  cursor = getReadableDatabase().rawQuery(Query,null);
            return cursor;
        }

    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // You do not need to implement this.
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        // You do not need to implement this.
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        /*
         * TODO: You need to implement this method. Note that values will have two columns (a key
         * column and a value column) and one row that contains the actual (key, value) pair to be
         * inserted.
         *
         * For actual storage, you can use any option. If you know how to use SQL, then you can use
         * SQLite. But this is not a requirement. You can use other storage options, such as the
         * internal storage option that we used in PA1. If you want to use that option, please
         * take a look at the code for PA1.
         */
        m_helper.Insert(values);
        Log.v("insert", values.toString());
        return uri;
    }

    @Override
    public boolean onCreate() {
        // If you need to perform any one-time initialization task, please do it here.
        try
        {
            Context context = getContext();
            m_helper=  new Databasehelper(context);
            Log.v("DB","Databasehelper created");
        }
        catch(Exception e)
        {
            Log.v("DB","Databasehelper is not created.Exception Occurred");
        }
        return false;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // You do not need to implement this.
        return 0;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        /*
         * TODO: You need to implement this method. Note that you need to return a Cursor object
         * with the right format. If the formatting is not correct, then it is not going to work.
         *
         * If you use SQLite, whatever is returned from SQLite is a Cursor object. However, you
         * still need to be careful because the formatting might still be incorrect.
         *
         * If you use a file storage option, then it is your job to build a Cursor * object. I
         * recommend building a MatrixCursor described at:
         * http://developer.android.com/reference/android/database/MatrixCursor.html
         */
        Cursor cursor = m_helper.Query(uri,projection,selection,selectionArgs,sortOrder);
        Log.v("query", selection);
        return cursor;
    }
}