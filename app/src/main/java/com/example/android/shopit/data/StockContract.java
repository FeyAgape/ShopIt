package com.example.android.shopit.data;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * API Contract for the ShopIt app.
 */
public final class StockContract {

    /**
     * The "Content authority" is a name for the entire content provider, similar to the
     * relationship between a domain name and its website.  A convenient string to use for the
     * content authority is the package name for the app, which is guaranteed to be unique on the
     * device.
     */
    public static final String CONTENT_AUTHORITY = "com.example.android.shopit";
    /**
     * Use CONTENT_AUTHORITY to create the base of all URI's which the apps will use to contact
     * the content provider.
     */
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);
    public static final String PATH_STOCK = "stock";


    // To prevent someone from accidentally instantiating the contract class,
    // give it an empty constructor.
    private StockContract() {
    }

    /**
     * Inner class that defines constant values for the stocks database table.
     * Each entry in the table represents a single stock item.
     */
    public static final class StockEntry implements BaseColumns {

        /**
         * The content URI to access the stock data in the provider
         */
        public static final Uri CONTENT_URI = Uri.withAppendedPath(BASE_CONTENT_URI, PATH_STOCK);

        /**
         * The MIME type of the {@link #CONTENT_URI} for a list of stocks.
         */
        public static final String CONTENT_LIST_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_STOCK;

        /**
         * The MIME type of the {@link #CONTENT_URI} for a single stock.
         */
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_STOCK;

        /**
         * Name of database table for stocks
         */
        public final static String TABLE_NAME = "stocks";

        /**
         * Unique ID number for each stock (only for use in the database table).
         * <p>
         * Type: INTEGER
         */
        public final static String _ID = BaseColumns._ID;

        /**
         * Name of the stock.
         * <p>
         * Type: TEXT
         */
        public final static String COLUMN_STOCK_NAME = "name";

        /**
         * Name of the stock supplier.
         * <p>
         * Type: TEXT
         */
        public final static String COLUMN_STOCK_SUPPLIER = "supplier";

        /**
         * Number of items in stock
         * <p>
         * Type: INTEGER
         */
        public final static String COLUMN_STOCK_QUANTITY = "quantity";

        /**
         * Type of stock.
         * <p>
         * The only possible values are {@link #TYPE_UNKNOWN}, {@link #TYPE_ONE},
         * or {@link #TYPE_TWO}.
         * <p>
         * Type: INTEGER
         */
        public final static String COLUMN_STOCK_TYPE = "type";

        /**
         * Price of the stock.
         * <p>
         * Type: INTEGER
         */
        public final static String COLUMN_STOCK_PRICE = "price";
        /**
         * Possible values for the type of stock.
         */
        public static final int TYPE_UNKNOWN = 0;
        public static final int TYPE_ONE = 1;
        public static final int TYPE_TWO = 2;
        /**
         * Image of the item
         * <p>
         * TYPE: BYTE
         */
        final static String COLUMN_STOCK_IMAGE = "image";

        /**
         * Returns whether or not the given type is {@link #TYPE_UNKNOWN}, {@link #TYPE_ONE},
         * or {@link #TYPE_TWO}.
         */
        public static boolean isValidType(int type) {
            if (type == TYPE_UNKNOWN || type == TYPE_ONE || type == TYPE_TWO) {
                return true;
            }
            return false;
        }

        public static boolean quantityNotZero(int quantity) {
            return quantity >= 0;
        }
    }

}