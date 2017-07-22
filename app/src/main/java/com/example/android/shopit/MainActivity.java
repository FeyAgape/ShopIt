package com.example.android.shopit;

import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.example.android.shopit.data.StockContract;

/**
 * Displays list of stocks that were entered and stored in the app.
 */
public class MainActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    /**
     * Identifier for the stock data loader
     */
    private static final int STOCK_LOADER = 0;

    /**
     * Adapter for the ListView
     */
    StockCursorAdapter mCursorAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Setup FAB to open EditorActivity
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, EditorActivity.class);
                startActivity(intent);
            }
        });

        // Find the ListView which will be populated with the stock data
        ListView stockListView = (ListView) findViewById(R.id.list);

        // Find and set empty view on the ListView, so that it only shows when the list has 0 items.
        View emptyView = findViewById(R.id.empty_view);
        stockListView.setEmptyView(emptyView);

        // Setup an Adapter to create a list item for each row of stock data in the Cursor.
        // There is no stock data yet (until the loader finishes) so pass in null for the Cursor.
        mCursorAdapter = new StockCursorAdapter(this, null);
        stockListView.setAdapter(mCursorAdapter);

        // Setup the item click listener
        stockListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                // Create new intent to go to {@link EditorActivity}
                Intent intent = new Intent(MainActivity.this, EditorActivity.class);

                // Form the content URI that represents the specific stock that was clicked on,
                // by appending the "id" (passed as input to this method) onto the
                // {@link StockEntry#CONTENT_URI}.
                // For example, the URI would be "content://com.example.android.shopit/stocks/2"
                // if the stock with ID 2 was clicked on.
                Uri currentStockUri = ContentUris.withAppendedId(StockContract.StockEntry.CONTENT_URI, id);

                // Set the URI on the data field of the intent
                intent.setData(currentStockUri);

                // Launch the {@link EditorActivity} to display the data for the current stock.
                startActivity(intent);
            }
        });

        // Kick off the loader
        getLoaderManager().initLoader(STOCK_LOADER, null, this);
    }

    /**
     * Helper method to insert hardcoded stock data into the database. For debugging purposes only.
     */
    private void insertStock() {
        // Create a ContentValues object where column names are the keys,
        // and Loreal Eyeliner stock attributes are the values.
        ContentValues values = new ContentValues();
        values.put(StockContract.StockEntry.COLUMN_STOCK_NAME, "Eyeliner");
        values.put(StockContract.StockEntry.COLUMN_STOCK_SUPPLIER, "Loreal");
        values.put(StockContract.StockEntry.COLUMN_STOCK_TYPE, StockContract.StockEntry.TYPE_TWO);
        values.put(StockContract.StockEntry.COLUMN_STOCK_PRICE, 5);
        values.put(StockContract.StockEntry.COLUMN_STOCK_QUANTITY, 10);

        // Insert a new row for Loreal Eyeliner into the provider using the ContentResolver.
        // Use the {@link StockEntry#CONTENT_URI} to indicate that we want to insert
        // into the stock database table.
        // Receive the new content URI that will allow us to access Loreal Eyeliner's data in the future.
        Uri newUri = getContentResolver().insert(StockContract.StockEntry.CONTENT_URI, values);
    }

    /**
     * Helper method to delete all stock in the database.
     */
    private void deleteAllStocks() {
        int rowsDeleted = getContentResolver().delete(StockContract.StockEntry.CONTENT_URI, null, null);
        Log.v("MainActivity", rowsDeleted + " rows deleted from stock database");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_inventory.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_inventory, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Insert dummy data" menu option
            case R.id.action_insert_dummy_data:
                insertStock();
                return true;
            // Respond to a click on the "Delete all entries" menu option
            case R.id.action_delete_all_entries:
                deleteAllStocks();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        // Define a projection that specifies the columns from the table we care about.
        String[] projection = {
                StockContract.StockEntry._ID,
                StockContract.StockEntry.COLUMN_STOCK_NAME,
                StockContract.StockEntry.COLUMN_STOCK_QUANTITY,
                StockContract.StockEntry.COLUMN_STOCK_TYPE,
                StockContract.StockEntry.COLUMN_STOCK_PRICE,
                StockContract.StockEntry.COLUMN_STOCK_SUPPLIER};


        // This loader will execute the ContentProvider's query method on a background thread
        return new CursorLoader(this,   // Parent activity context
                StockContract.StockEntry.CONTENT_URI,   // Provider content URI to query
                projection,             // Columns to include in the resulting Cursor
                null,                   // No selection clause
                null,                   // No selection arguments
                null);                  // Default sort order
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Update {@link StockCursorAdapter} with this new cursor containing updated stock data
        mCursorAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // Callback called when the data needs to be deleted
        mCursorAdapter.swapCursor(null);
    }
}