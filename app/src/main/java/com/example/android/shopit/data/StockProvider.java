package com.example.android.shopit.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import com.example.android.shopit.data.StockContract.StockEntry;

/**
 * Created by FEY-AGAPE on 20/07/2017.
 */

/**
 * {@link ContentProvider} for ShopIt app.
 */
public class StockProvider extends ContentProvider {

    /**
     * Tag for the log messages
     */
    public static final String LOG_TAG = StockProvider.class.getSimpleName();

    /**
     * URI matcher code for the content URI for the stocks table
     */
    private static final int STOCKS = 100;

    /**
     * URI matcher code for the content URI for a single stock item in the stocks table
     */
    private static final int STOCK_ID = 101;

    /**
     * UriMatcher object to match a content URI to a corresponding code.
     * The input passed into the constructor represents the code to return for the root URI.
     * It's common to use NO_MATCH as the input for this case.
     */
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    // Static initializer. This is run the first time anything is called from this class.
    static {
        // The calls to addURI() go here, for all of the content URI patterns that the provider
        // should recognize. All paths added to the UriMatcher have a corresponding code to return
        // when a match is found.

        // The content URI of the form "content://com.example.android.shopit/stocks" will map to the
        // integer code {@link #STOCKS}. This URI is used to provide access to MULTIPLE rows
        // of the stocks table.
        sUriMatcher.addURI(StockContract.CONTENT_AUTHORITY, StockContract.PATH_STOCK, STOCKS);
        //
        // In this case, the "#" wildcard is used where "#" can be substituted for an integer.
        // For example, "content://com.example.android.shopit/stocks/3" matches, but
        // "content://com.example.android.shopit/stocks" (without a number at the end) doesn't match.
        sUriMatcher.addURI(StockContract.CONTENT_AUTHORITY, StockContract.PATH_STOCK + "/#", STOCK_ID);
    }

    /**
     * Database helper object
     */
    private StockDbHelper mDbHelper;

    @Override
    public boolean onCreate() {
        mDbHelper = new StockDbHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        // Get readable database
        SQLiteDatabase database = mDbHelper.getReadableDatabase();

        // This cursor will hold the result of the query
        Cursor cursor;

        // Figure out if the URI matcher can match the URI to a specific code
        int match = sUriMatcher.match(uri);
        switch (match) {
            case STOCKS:
                // For the STOCKS code, query the stocks table directly with the given
                // projection, selection, selection arguments, and sort order. The cursor
                // could contain multiple rows of the stocks table.
                cursor = database.query(StockContract.StockEntry.TABLE_NAME, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;
            case STOCK_ID:
                // For the STOCK_ID code, extract out the ID from the URI.
                //
                //
                // For every "?" in the selection, we need to have an element in the selection
                // arguments that will fill in the "?". Since we have 1 question mark in the
                // selection, we have 1 String in the selection arguments' String array.
                selection = StockContract.StockEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};

                // This will perform a query on the stocks table where the _id equals 3 to return a
                // Cursor containing that row of the table.
                cursor = database.query(StockEntry.TABLE_NAME, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;
            default:
                throw new IllegalArgumentException("Cannot query unknown URI " + uri);
        }

        // Set notification URI on the Cursor,
        // so we know what content URI the Cursor was created for.
        // If the data at this URI changes, then we know we need to update the Cursor.
        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        // Return the cursor
        return cursor;
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case STOCKS:
                return insertStock(uri, contentValues);
            default:
                throw new IllegalArgumentException("Insertion is not supported for " + uri);
        }
    }

    /**
     * Insert a stock into the database with the given content values. Return the new content URI
     * for that specific row in the database.
     */
    private Uri insertStock(Uri uri, ContentValues values) {
        // Check that the name is not null
        String name = values.getAsString(StockContract.StockEntry.COLUMN_STOCK_NAME);
        if (name == null) {
            throw new IllegalArgumentException("Stock requires a name");
        }

        // Check that the type is valid
        Integer type = values.getAsInteger(StockContract.StockEntry.COLUMN_STOCK_TYPE);
        if (type == null || !StockContract.StockEntry.isValidType(type)) {
            throw new IllegalArgumentException("Stock requires valid type");
        }

        // Check that the quantity is not less than 0
        Integer quantity = values.getAsInteger(StockContract.StockEntry.COLUMN_STOCK_QUANTITY);

        if (quantity != null && !StockContract.StockEntry.quantityNotZero(quantity))
            throw new IllegalArgumentException("Stock cannot have a negative quantity");

        // If the price is provided, check that it's greater than or equal to £0
        Integer price = values.getAsInteger(StockContract.StockEntry.COLUMN_STOCK_PRICE);
        if (price != null && price < 0) {
            throw new IllegalArgumentException("Stock requires valid price");
        }

        // No need to check the Supplier, any value is valid (including null).

        // Get writeable database
        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        // Insert the new stock with the given values
        long id = database.insert(StockContract.StockEntry.TABLE_NAME, null, values);
        // If the ID is -1, then the insertion failed. Log an error and return null.
        if (id == -1) {
            Log.e(LOG_TAG, "Failed to insert row for " + uri);
            return null;
        }

        // Notify all listeners that the data has changed for the stock content URI
        getContext().getContentResolver().notifyChange(uri, null);

        // Return the new URI with the ID (of the newly inserted row) appended at the end
        return ContentUris.withAppendedId(uri, id);
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String selection,
                      String[] selectionArgs) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case STOCKS:
                return updateStock(uri, contentValues, selection, selectionArgs);
            case STOCK_ID:
                // For the STOCK_ID code, extract out the ID from the URI,
                // so we know which row to update. Selection will be "_id=?" and selection
                // arguments will be a String array containing the actual ID.
                selection = StockContract.StockEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                return updateStock(uri, contentValues, selection, selectionArgs);
            default:
                throw new IllegalArgumentException("Update is not supported for " + uri);
        }
    }

    /**
     * Update stocks in the database with the given content values. Apply the changes to the rows
     * specified in the selection and selection arguments (which could be 0 or 1 or more stocks).
     * Return the number of rows that were successfully updated.
     */
    private int updateStock(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // If the {@link StockEntry#COLUMN_STOCK_NAME} key is present,
        // check that the name value is not null.
        if (values.containsKey(StockContract.StockEntry.COLUMN_STOCK_NAME)) {
            String name = values.getAsString(StockContract.StockEntry.COLUMN_STOCK_NAME);
            if (name == null) {
                throw new IllegalArgumentException("Stock requires a name");
            }
        }

        // If the {@link StockEntry#COLUMN_STOCK_TYPE} key is present,
        // check that the type value is valid.
        if (values.containsKey(StockContract.StockEntry.COLUMN_STOCK_TYPE)) {
            Integer type = values.getAsInteger(StockContract.StockEntry.COLUMN_STOCK_TYPE);
            if (type == null || !StockContract.StockEntry.isValidType(type)) {
                throw new IllegalArgumentException("Stock requires valid type");
            }
        }

        // If the {@link StockEntry#COLUMN_STOCK_PRICE} key is present,
        // check that the price is valid.
        if (values.containsKey(StockContract.StockEntry.COLUMN_STOCK_PRICE)) {
            // Check that the price is greater than or equal to £0
            Integer price = values.getAsInteger(StockContract.StockEntry.COLUMN_STOCK_PRICE);
            if (price != null && !StockContract.StockEntry.quantityNotZero(price)) {
                throw new IllegalArgumentException("Stock requires valid price");
            }
        }

        // No need to check the supplier any value is valid (including null).

        // If there are no values to update, then don't try to update the database
        if (values.size() == 0) {
            return 0;
        }

        // Otherwise, get writeable database to update the data
        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        // Perform the update on the database and get the number of rows affected
        int rowsUpdated = database.update(StockContract.StockEntry.TABLE_NAME, values, selection, selectionArgs);

        // If 1 or more rows were updated, then notify all listeners that the data at the
        // given URI has changed
        if (rowsUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        // Return the number of rows updated
        return rowsUpdated;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // Get writeable database
        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        // Track the number of rows that were deleted
        int rowsDeleted;

        final int match = sUriMatcher.match(uri);
        switch (match) {
            case STOCKS:
                // Delete all rows that match the selection and selection args
                rowsDeleted = database.delete(StockContract.StockEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case STOCK_ID:
                // Delete a single row given by the ID in the URI
                selection = StockContract.StockEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                rowsDeleted = database.delete(StockContract.StockEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Deletion is not supported for " + uri);
        }

        // If 1 or more rows were deleted, then notify all listeners that the data at the
        // given URI has changed
        if (rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        // Return the number of rows deleted
        return rowsDeleted;
    }

    @Override
    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case STOCKS:
                return StockContract.StockEntry.CONTENT_LIST_TYPE;
            case STOCK_ID:
                return StockEntry.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalStateException("Unknown URI " + uri + " with match " + match);
        }
    }
}