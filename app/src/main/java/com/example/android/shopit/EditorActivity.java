package com.example.android.shopit;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NavUtils;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.shopit.data.StockContract;
import com.example.android.shopit.data.StockContract.StockEntry;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Allows user to create a new stork or edit an existing one.
 */
public class EditorActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private static final String FILE_PROVIDER_AUTHORITY = "com.example.android.shopit.data.StockContract.StockEntry";

    private static final String LOG_TAG = EditorActivity.class.getSimpleName();

    //Identifier for the stork data loader
    private static final int EXISTING_STOCK_LOADER = 0;
    private static final int STOCK_IMAGE = 0;
    private static final int PERMISSIONS_REQUEST = 2;


    //Content URI for the existing stork (null if it's new)
    private Uri mCurrentStockUri;

    //Image related
    private ImageView mStockImageView;
    private ImageButton mCameraButton;
    private Uri mImageUri;

    //EditText fields
    private EditText mNameEditText;
    private EditText mSupplierEditText;
    private EditText mPriceEditText;

    //TextView and ImageButton's for the quantity amount
    private TextView mQuantityTextView;
    private ImageButton mReduceQuantity;
    private ImageButton mIncreaseQuantity;
    private int quantity;

    //Supplier order more ImageButton
    private ImageButton mOrderMore;

    //Field to enter the stock's type
    private Spinner mTypeSpinner;

    /**
     * Type of stock. The possible valid values are in the StockContract.java file:
     * {@link StockEntry#TYPE_UNKNOWN}, {@link StockEntry#TYPE_ONE}, or
     * {@link StockEntry#TYPE_TWO}.
     */
    private int mType = StockEntry.TYPE_UNKNOWN;

    // Boolean flag that keeps track of the stock (true) or not (false)
    private boolean mStockHasChanged = false;
    private boolean mStockSaved = false;

    /**
     * OnTouchListener that listens for any user touches on a View, implying that they are modifying
     * the view, and we change the mStockHasChanged boolean to true.
     */
    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent event) {
            mStockHasChanged = true;
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        // Examine the intent that was used to launch this activity,
        // in order to figure out if we're creating a new stock or editing an existing one.
        Intent intent = getIntent();
        mCurrentStockUri = intent.getData();

        // If the intent DOES NOT contain a stock content URI, then we know that we are
        // creating a new stock item.
        if (mCurrentStockUri == null) {
            // This is a new stock, so change the app bar to say "Add a Stock"
            setTitle(getString(R.string.editor_activity_title_new_stock));

            // Invalidate the options menu, so the "Delete" menu option can be hidden.
            // (It doesn't make sense to delete a stock that hasn't been created yet.)
            invalidateOptionsMenu();
        } else {
            // Otherwise this is an existing stock, so change app bar to say "Edit Stock"
            setTitle(getString(R.string.editor_activity_title_edit_stock));

            // Initialize a loader to read the pet data from the database
            // and display the current values in the editor
            getLoaderManager().initLoader(EXISTING_STOCK_LOADER, null, this);
        }

        // Find all relevant views that we will need to read user input from
        mStockImageView = (ImageView) findViewById(R.id.stock_image);
        mCameraButton = (ImageButton) findViewById(R.id.camera);
        mNameEditText = (EditText) findViewById(R.id.edit_stock_name);
        mSupplierEditText = (EditText) findViewById(R.id.edit_stock_supplier);
        mPriceEditText = (EditText) findViewById(R.id.edit_stock_price);
        mIncreaseQuantity = (ImageButton) findViewById(R.id.add);
        mReduceQuantity = (ImageButton) findViewById(R.id.remove);
        mQuantityTextView = (TextView) findViewById(R.id.quantity_textView);
        mTypeSpinner = (Spinner) findViewById(R.id.spinner_type);
        mOrderMore = (ImageButton) findViewById(R.id.supplier_order_more);

        mCameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Invoke method for opening an image folder
                requestPermissions();
                mStockHasChanged = true;
            }
        });

        // Setup OnTouchListeners on all the input fields, so we can determine if the user
        // has touched or modified them. This will let us know if there are unsaved changes
        // or not, if the user tries to leave the editor without saving.
        mStockImageView.setOnTouchListener(mTouchListener);
        mNameEditText.setOnTouchListener(mTouchListener);
        mSupplierEditText.setOnTouchListener(mTouchListener);
        mPriceEditText.setOnTouchListener(mTouchListener);
        mIncreaseQuantity.setOnTouchListener(mTouchListener);
        mReduceQuantity.setOnTouchListener(mTouchListener);
        mQuantityTextView.setOnTouchListener(mTouchListener);
        mTypeSpinner.setOnTouchListener(mTouchListener);
        mOrderMore.setOnTouchListener(mTouchListener);

        setupSpinner();
        orderMore();
        reduceQuantity();
        increaseQuantity();
    }

    /**
     * Setup the dropdown spinner that allows the user to select the type of stock.
     */
    private void setupSpinner() {
        // Create adapter for spinner. The list options are from the String array it will use
        // the spinner will use the default layout
        ArrayAdapter typeSpinnerAdapter = ArrayAdapter.createFromResource(this,
                R.array.array_type_options, android.R.layout.simple_spinner_item);

        // Specify dropdown layout style - simple list view with 1 item per line
        typeSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);

        // Apply the adapter to the spinner
        mTypeSpinner.setAdapter(typeSpinnerAdapter);

        // Set the integer mSelected to the constant values
        mTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selection = (String) parent.getItemAtPosition(position);
                if (!TextUtils.isEmpty(selection)) {
                    if (selection.equals(getString(R.string.type_one))) {
                        mType = StockContract.StockEntry.TYPE_ONE;
                    } else if (selection.equals(getString(R.string.type_two))) {
                        mType = StockContract.StockEntry.TYPE_TWO;
                    } else {
                        mType = StockContract.StockEntry.TYPE_UNKNOWN;
                    }
                }
            }

            // Because AdapterView is an abstract class, onNothingSelected must be defined
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                mType = StockContract.StockEntry.TYPE_UNKNOWN;
            }
        });
    }

    // open an email when the user clicks the supplier order more button
    private void orderMore() {
        mOrderMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String message = getString(R.string.email_body)
                        + mNameEditText.getText().toString().trim() + "\n";

                message = message + getString(R.string.email_extra);

                Intent intent = new Intent(Intent.ACTION_SENDTO);
                intent.setData(Uri.parse("mailto:")); // only email apps should handle this

                intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.email_subject));
                intent.putExtra(Intent.EXTRA_TEXT, message);

                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                }
            }
        });
    }

    /**
     * Setup the minus quantity button that allows the user to control the number of stock.
     */
    private void reduceQuantity() {
        mReduceQuantity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mQuantityTextView.getText().toString().equals(null) ||
                        mQuantityTextView.getText().toString().equals("")) {
                    Toast.makeText(EditorActivity.this, getString(R.string.no_quantity),
                            Toast.LENGTH_SHORT).show();
                } else if (quantity < 2) {
                    Toast.makeText(EditorActivity.this, getString(R.string.negative_quantity),
                            Toast.LENGTH_SHORT).show();
                } else {
                    quantity = Integer.parseInt(mQuantityTextView.getText().toString());
                    mQuantityTextView.setText(String.valueOf(quantity - 1));
                }
            }
        });
    }

    /**
     * Setup the plus quantity button that allows the user to control the number of stock.
     */
    private void increaseQuantity() {
        mIncreaseQuantity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mQuantityTextView.getText().toString().equals(null) ||
                        mQuantityTextView.getText().toString().equals("")) {
                    Toast.makeText(EditorActivity.this, getString(R.string.no_quantity),
                            Toast.LENGTH_SHORT).show();
                } else if (quantity > 997) {
                    Toast.makeText(EditorActivity.this, getString(R.string.quantity_limit),
                            Toast.LENGTH_SHORT).show();
                } else {
                    quantity = Integer.parseInt(mQuantityTextView.getText().toString());
                    mQuantityTextView.setText(String.valueOf(quantity + 1));
                }
            }
        });
    }

    /**
     * Toast message for when the sale button is pressed in the ListView.
     */
    public void onSalePress(View view) {

        Toast.makeText(this, R.string.sale_button, Toast.LENGTH_SHORT).show();
    }

    /**
     * Method for making image request.
     */
    public void requestPermissions() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE) ||
                    ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                ActivityCompat.requestPermissions(this, new String[]{
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        PERMISSIONS_REQUEST);
            }
        } else {
            mCameraButton.setEnabled(true);
            openImageSelector();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    openImageSelector();
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == STOCK_IMAGE && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                mImageUri = data.getData();
                Log.v(LOG_TAG, "Uri: " + mImageUri);

                mStockImageView.setImageURI(mImageUri);
                mStockImageView.setImageBitmap(getBitmapFromUri(mImageUri));
                mStockImageView.invalidate();
            }
        }
    }

    public Bitmap getBitmapFromUri(Uri uri) {
        if (uri == null || uri.toString().isEmpty()) {
            return null;
        }

        // Get the dimensions of the View
        int targetWidth = mStockImageView.getWidth();
        int targetHeight = mStockImageView.getHeight();

        InputStream inputStream = null;
        try {
            inputStream = this.getContentResolver().openInputStream(uri);

            // Get the size of the bitmap
            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            bmOptions.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(inputStream, null, bmOptions);
            inputStream.close();

            int photoWidth = bmOptions.outWidth;
            int photoHeight = bmOptions.outHeight;

            // Determine the image size
            int scaleFactor = Math.min(photoWidth / targetWidth, photoHeight / targetHeight);

            // Scale the image file into a Bitmap sized to fill the View
            bmOptions.inJustDecodeBounds = false;
            bmOptions.inSampleSize = scaleFactor;
            bmOptions.inPurgeable = true;

            inputStream = this.getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, bmOptions);
            inputStream.close();
            return bitmap;

        } catch (FileNotFoundException noFile) {
            Log.e(LOG_TAG, "Failed to load image.", noFile);
            return null;
        } catch (Exception e) {
            Log.e(LOG_TAG, "Failed to load image.", e);
            return null;
        } finally {
            try {
                inputStream.close();
            } catch (IOException ioe) {
            }
        }
    }

    private void openImageSelector() {
        Intent intent;
        if (Build.VERSION.SDK_INT < 19) {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
        } else {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
        }

        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), STOCK_IMAGE);
    }

    /**
     * Get user input from editor and save stock into database.
     */
    private boolean saveStock() {
        // Read from input fields
        // Use trim to eliminate leading or trailing white space
        String nameString = mNameEditText.getText().toString().trim();
        String supplierString = mSupplierEditText.getText().toString().trim();
        String quantityString = mQuantityTextView.getText().toString().trim();
        String priceString = mPriceEditText.getText().toString().trim();


        // Check if this is supposed to be a new stock
        // and check if all the fields in the editor are blank
        if (mCurrentStockUri == null &&
                TextUtils.isEmpty(nameString) &&
                TextUtils.isEmpty(supplierString) &&
                TextUtils.isEmpty(quantityString) &&
                TextUtils.isEmpty(priceString) &&
                mType == StockEntry.TYPE_UNKNOWN &&
                mImageUri == null) {
            // Since no fields were modified, we can return early without creating a new stock.
            // No need to create ContentValues and no need to do any ContentProvider operations.
            Toast.makeText(this, getString(R.string.require_attributes),
                    Toast.LENGTH_SHORT).show();
            mStockSaved = false;
            return mStockSaved;
        }

        // Create a ContentValues object where column names are the keys,
        // and stork attributes from the editor are the values.
        ContentValues values = new ContentValues();
        if (mImageUri == null) {
            Toast.makeText(this, getString(R.string.require_image),
                    Toast.LENGTH_SHORT).show();
            mStockSaved = false;
            return mStockSaved;
        }
        values.put(StockEntry.COLUMN_STOCK_IMAGE, mImageUri.toString());

        if (TextUtils.isEmpty(nameString)) {
            Toast.makeText(this, getString(R.string.require_name),
                    Toast.LENGTH_SHORT).show();
            mStockSaved = false;
            return mStockSaved;
        }
        values.put(StockEntry.COLUMN_STOCK_NAME, nameString);

        values.put(StockEntry.COLUMN_STOCK_SUPPLIER, supplierString);
        values.put(StockEntry.COLUMN_STOCK_QUANTITY, quantityString);
        values.put(StockEntry.COLUMN_STOCK_TYPE, mType);

        if (!TextUtils.isEmpty(quantityString)) {
            quantity = Integer.parseInt(quantityString);
        }
        values.put(StockEntry.COLUMN_STOCK_QUANTITY, quantity);
        // If the price is not provided by the user, don't try to parse the string into an
        // integer value. Use 0 by default.
        int price = 0;
        if (!TextUtils.isEmpty(priceString)) {
            price = Integer.parseInt(priceString);
        }
        values.put(StockEntry.COLUMN_STOCK_PRICE, price);

        // Determine if this is a new or existing price by checking if mCurrentStockUri is null or not
        if (mCurrentStockUri == null) {
            // This is a NEW stock, so insert a new stock into the provider,
            // returning the content URI for the new stock.
            Uri newUri = getContentResolver().insert(StockEntry.CONTENT_URI, values);

            // Show a toast message depending on whether or not the insertion was successful.
            if (newUri == null) {
                // If the new content URI is null, then there was an error with insertion.
                Toast.makeText(this, getString(R.string.editor_insert_stock_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the insertion was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.editor_insert_stock_successful),
                        Toast.LENGTH_SHORT).show();
            }
        } else {
            // Otherwise this is an EXISTING stock, so update the stock with content URI: mCurrentStockUri
            // and pass in the new ContentValues. Pass in null for the selection and selection args
            // because mCurrentStockUri will already identify the correct row in the database that
            // we want to modify.
            int rowsAffected = getContentResolver().update(mCurrentStockUri, values, null, null);

            // Show a toast message depending on whether or not the update was successful.
            if (rowsAffected == 0) {
                // If no rows were affected, then there was an error with the update.
                Toast.makeText(this, getString(R.string.editor_update_stock_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the update was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.editor_update_stock_successful),
                        Toast.LENGTH_SHORT).show();
            }
        }
        mStockSaved = true;
        return mStockSaved;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_editor.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        return true;
    }

    /**
     * This method is called after invalidateOptionsMenu(), so that the
     * menu can be updated (some menu items can be hidden or made visible).
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        // If this is a new stock, hide the "Delete" menu item.
        if (mCurrentStockUri == null) {
            MenuItem menuItem = menu.findItem(R.id.action_delete);
            menuItem.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Save" menu option
            case R.id.action_save:
                // Save stock to database
                if (saveStock()) {
                    // Exit activity
                    finish();
                }
                return true;
            // Respond to a click on the "Delete" menu option
            case R.id.action_delete:
                // Pop up confirmation dialog for deletion
                showDeleteConfirmationDialog();
                return true;
            // Respond to a click on the "Up" arrow button in the app bar
            case android.R.id.home:
                // If the stock hasn't changed, continue with navigating up to parent activity
                // which is the {@link MainActivity}.
                if (!mStockHasChanged) {
                    NavUtils.navigateUpFromSameTask(EditorActivity.this);
                    return true;
                }

                // Otherwise if there are unsaved changes, setup a dialog to warn the user.
                // Create a click listener to handle the user confirming that
                // changes should be discarded.
                DialogInterface.OnClickListener discardButtonClickListener =
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // User clicked "Discard" button, navigate to parent activity.
                                NavUtils.navigateUpFromSameTask(EditorActivity.this);
                            }
                        };

                // Show a dialog that notifies the user they have unsaved changes
                showUnsavedChangesDialog(discardButtonClickListener);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * This method is called when the back button is pressed.
     */
    @Override
    public void onBackPressed() {
        // If the stock hasn't changed, continue with handling back button press
        if (!mStockHasChanged) {
            super.onBackPressed();
            return;
        }

        // Otherwise if there are unsaved changes, setup a dialog to warn the user.
        // Create a click listener to handle the user confirming that changes should be discarded.
        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // User clicked "Discard" button, close the current activity.
                        finish();
                    }
                };

        // Show dialog that there are unsaved changes
        showUnsavedChangesDialog(discardButtonClickListener);
    }


    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        // Since the editor shows all stock attributes, define a projection that contains
        // all columns from the stock table
        String[] projection = {
                StockEntry._ID,
                StockEntry.COLUMN_STOCK_IMAGE,
                StockEntry.COLUMN_STOCK_NAME,
                StockEntry.COLUMN_STOCK_SUPPLIER,
                StockEntry.COLUMN_STOCK_TYPE,
                StockEntry.COLUMN_STOCK_QUANTITY,
                StockEntry.COLUMN_STOCK_PRICE};

        // This loader will execute the ContentProvider's query method on a background thread
        return new CursorLoader(this,   // Parent activity context
                mCurrentStockUri,         // Query the content URI for the current stock
                projection,             // Columns to include in the resulting Cursor
                null,                   // No selection clause
                null,                   // No selection arguments
                null);                  // Default sort order
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        Log.v(LOG_TAG, "Now is onLoadFinished called");
        // Bail early if the cursor is null or there is less than 1 row in the cursor
        if (cursor == null || cursor.getCount() < 1) {
            return;
        }

        // Proceed with moving to the first row of the cursor and reading data from it
        // (This should be the only row in the cursor)
        if (cursor.moveToFirst()) {
            // Find the columns of stock attributes that we're interested in
            int imageColumnIndex = cursor.getColumnIndex(StockEntry.COLUMN_STOCK_IMAGE);
            int nameColumnIndex = cursor.getColumnIndex(StockEntry.COLUMN_STOCK_NAME);
            int supplierColumnIndex = cursor.getColumnIndex(StockEntry.COLUMN_STOCK_SUPPLIER);
            int typeColumnIndex = cursor.getColumnIndex(StockEntry.COLUMN_STOCK_TYPE);
            int quantityColumnIndex = cursor.getColumnIndex(StockEntry.COLUMN_STOCK_QUANTITY);
            int priceColumnIndex = cursor.getColumnIndex(StockEntry.COLUMN_STOCK_PRICE);

            // Extract out the value from the Cursor for the given column index
            String imageUriString = cursor.getString(imageColumnIndex);
            String name = cursor.getString(nameColumnIndex);
            String supplier = cursor.getString(supplierColumnIndex);
            int type = cursor.getInt(typeColumnIndex);
            quantity = cursor.getInt(quantityColumnIndex);
            int price = cursor.getInt(priceColumnIndex);

            // Update the views on the screen with the values from the database
            mImageUri = Uri.parse(imageUriString);
            mStockImageView.setImageURI(mImageUri);
            mNameEditText.setText(name);
            mSupplierEditText.setText(supplier);
            mQuantityTextView.setText(Integer.toString(quantity));
            mPriceEditText.setText(Integer.toString(price));

            // type is a dropdown spinner, so map the constant value from the database
            // into one of the dropdown options (0 is Unknown, 1 is Perishable, 2 is Non-perishable).
            // Then call setSelection() so that option is displayed on screen as the current selection.
            switch (type) {
                case StockContract.StockEntry.TYPE_ONE:
                    mTypeSpinner.setSelection(1);
                    break;
                case StockContract.StockEntry.TYPE_TWO:
                    mTypeSpinner.setSelection(2);
                    break;
                default:
                    mTypeSpinner.setSelection(0);
                    break;
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        Log.v(LOG_TAG, "Now is onLoaderReset called");
        // If the loader is invalidated, clear out all the data from the input fields.
        mStockImageView.setImageURI(mImageUri);
        mNameEditText.setText("");
        mSupplierEditText.setText("");
        mQuantityTextView.setText("");
        mPriceEditText.setText("");
        mTypeSpinner.setSelection(0); // Select "Unknown" type
    }

    /**
     * Show a dialog that warns the user there are unsaved changes that will be lost
     * if they continue leaving the editor.
     *
     * @param discardButtonClickListener is the click listener for what to do when
     *                                   the user confirms they want to discard their changes
     */
    private void showUnsavedChangesDialog(
            DialogInterface.OnClickListener discardButtonClickListener) {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the positive and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.unsaved_changes_dialog_msg);
        builder.setPositiveButton(R.string.discard, discardButtonClickListener);
        builder.setNegativeButton(R.string.keep_editing, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Keep editing" button, so dismiss the dialog
                // and continue editing the stock.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * Prompt the user to confirm that they want to delete this stock.
     */
    private void showDeleteConfirmationDialog() {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the positive and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_dialog_msg);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Delete" button, so delete the stock.
                deleteStock();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Cancel" button, so dismiss the dialog
                // and continue editing the stock.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * Perform the deletion of the stock in the database.
     */
    private void deleteStock() {
        // Only perform the delete if this is an existing stock.
        if (mCurrentStockUri != null) {
            // Call the ContentResolver to delete the stock at the given content URI.
            // Pass in null for the selection and selection args because the mCurrentStockUri
            // content URI already identifies the stock item that we want.
            int rowsDeleted = getContentResolver().delete(mCurrentStockUri, null, null);

            // Show a toast message depending on whether or not the delete was successful.
            if (rowsDeleted == 0) {
                // If no rows were deleted, then there was an error with the delete.
                Toast.makeText(this, getString(R.string.editor_delete_stock_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the delete was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.editor_delete_stock_successful),
                        Toast.LENGTH_SHORT).show();
            }
        }

        // Close the activity
        finish();
    }
}