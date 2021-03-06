package com.example.android.shopit;

/**
 * Created by FEY-AGAPE on 20/07/2017.
 */

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.shopit.data.StockContract.StockEntry;

import static com.example.android.shopit.R.string.quantity;
import static java.lang.Integer.parseInt;

/**
 * {@link StockCursorAdapter} is an adapter for a list or grid view
 * that uses a {@link Cursor} of stock data as its data source. This adapter knows
 * how to create list items for each row of stock data in the {@link Cursor}.
 */
public class StockCursorAdapter extends CursorAdapter {

    /**
     * Constructs a new {@link StockCursorAdapter}.
     *
     * @param context The context
     * @param c       The cursor from which to get the data.
     */
    public StockCursorAdapter(Context context, Cursor c) {
        super(context, c, 0 /* flags */);
    }

    /**
     * Makes a new blank list item view. No data is set (or bound) to the views yet.
     *
     * @param context app context
     * @param cursor  The cursor from which to get the data. The cursor is already
     *                moved to the correct position.
     * @param parent  The parent to which the new view is attached to
     * @return the newly created list item view.
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        // Inflate a list item view using the layout specified in list_item.xml
        return LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);
    }

    /**
     * This method binds the stock data (in the current row pointed to by cursor) to the given
     * list item layout. For example, the name for the current stock iten can be set on the name TextView
     * in the list item layout.
     *
     * @param view    Existing view, returned earlier by newView() method
     * @param context app context
     * @param cursor  The cursor from which to get the data. The cursor is already moved to the
     *                correct row.
     */
    @Override
    public void bindView(View view, final Context context, Cursor cursor) {
        // Find individual views that we want to modify in the list item layout
        TextView nameTextView = (TextView) view.findViewById(R.id.name);
        TextView supplierTextView = (TextView) view.findViewById(R.id.supplier);
        TextView priceTextView = (TextView) view.findViewById(R.id.item_price);
        final TextView quantityTextView = (TextView) view.findViewById(R.id.item_quantity);
        ImageButton saleButton = (ImageButton) view.findViewById(R.id.sale_button);


        // Find the columns of stock attributes that we're interested in
        final String nameColumnIndex = cursor.getString(cursor.getColumnIndex(StockEntry.COLUMN_STOCK_NAME));
        int supplierColumnIndex = cursor.getColumnIndex(StockEntry.COLUMN_STOCK_SUPPLIER);
        final String quantityColumnIndex = cursor.getString(cursor.getColumnIndex(StockEntry.COLUMN_STOCK_QUANTITY));
        int priceColumnIndex = cursor.getColumnIndex(StockEntry.COLUMN_STOCK_PRICE);
        final long id = cursor.getLong(cursor.getColumnIndex(StockEntry._ID));

        // buy button listener

        saleButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                int stockQuantity = parseInt(quantityTextView.getText().toString());
                stockQuantity -= 1;

                if (stockQuantity < 0) {
                    stockQuantity = 0;
                }
                quantityTextView.setText(Integer.toString(stockQuantity));

                // find values to update in database
                ContentValues values = new ContentValues();
                values.put(StockEntry.COLUMN_STOCK_QUANTITY, stockQuantity);

                // find current stork URI
                Uri currentStockUri = ContentUris.withAppendedId(StockEntry.CONTENT_URI, id);

                // update affected row in database
                int rowAffected = context.getContentResolver().update(currentStockUri, values, null, null);
                Log.v("StockCursor", "Number of rows affected: " + rowAffected);
                if (rowAffected != 0 && stockQuantity != 0) {
                    Toast.makeText(context, R.string.sale_successful, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, R.string.sale_unsuccessful, Toast.LENGTH_SHORT).show();
                }
            }
        });


        // Read the stock attributes from the Cursor for the current stock
        String stockName = (nameColumnIndex);
        String stockSupplier = cursor.getString(supplierColumnIndex);
        String stockQuantity = (quantityColumnIndex);
        String stockPrice = cursor.getString(priceColumnIndex);

        // If the stock supplier is empty string or null, then use some default text
        // that says "Unknown supplier", so the TextView isn't blank.
        if (TextUtils.isEmpty(stockSupplier)) {
            stockSupplier = context.getString(R.string.unknown_type);

            if (TextUtils.isEmpty(stockPrice))
                stockPrice = context.getString(quantity);

        }

        // Update the TextViews with the attributes for the current stock
        nameTextView.setText(stockName);
        supplierTextView.setText(stockSupplier);
        quantityTextView.setText(stockQuantity);
        priceTextView.setText(stockPrice);
    }
}