package com.example.smartfoodexpirytracker;

import android.app.DatePickerDialog;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    EditText etFoodName, etQuantity;
    Button btnExpiryDate, btnAddFood;
    ListView listFoodItems;
    TextView tvFoodCount;

    DatabaseHelper dbHelper;

    ArrayList<String> foodList;
    ArrayList<Integer> foodIds;

    ArrayAdapter<String> adapter;

    String selectedExpiryDate = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etFoodName = findViewById(R.id.etFoodName);
        etQuantity = findViewById(R.id.etQuantity);
        btnExpiryDate = findViewById(R.id.btnExpiryDate);
        btnAddFood = findViewById(R.id.btnAddFood);
        listFoodItems = findViewById(R.id.listFoodItems);
        tvFoodCount = findViewById(R.id.tvFoodCount);

        dbHelper = new DatabaseHelper(this);

        foodList = new ArrayList<>();
        foodIds = new ArrayList<>();

        adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                foodList
        );

        listFoodItems.setAdapter(adapter);

        btnExpiryDate.setOnClickListener(v -> showDatePicker());

        btnAddFood.setOnClickListener(v -> addFood());

        listFoodItems.setOnItemLongClickListener(
                (parent, view, position, id) -> {

                    int foodId = foodIds.get(position);

                    boolean deleted = dbHelper.deleteFood(foodId);

                    if (deleted) {
                        Toast.makeText(
                                MainActivity.this,
                                "Food item deleted",
                                Toast.LENGTH_SHORT
                        ).show();

                        displayFoodItems();
                    } else {
                        Toast.makeText(
                                MainActivity.this,
                                "Unable to delete item",
                                Toast.LENGTH_SHORT
                        ).show();
                    }

                    return true;
                }
        );

        displayFoodItems();
    }

    private void showDatePicker() {

        Calendar calendar = Calendar.getInstance();

        DatePickerDialog datePickerDialog =
                new DatePickerDialog(
                        this,
                        (view, year, month, dayOfMonth) -> {

                            selectedExpiryDate = String.format(
                                    Locale.US,
                                    "%04d-%02d-%02d",
                                    year,
                                    month + 1,
                                    dayOfMonth
                            );

                            btnExpiryDate.setText(
                                    "Expiry Date: " + selectedExpiryDate
                            );
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                );

        datePickerDialog.show();
    }

    private void addFood() {

        String name = etFoodName.getText().toString().trim();
        String quantity = etQuantity.getText().toString().trim();

        if (name.isEmpty()) {
            etFoodName.setError("Enter food name");
            etFoodName.requestFocus();
            return;
        }

        if (quantity.isEmpty()) {
            etQuantity.setError("Enter quantity");
            etQuantity.requestFocus();
            return;
        }

        if (selectedExpiryDate.isEmpty()) {
            Toast.makeText(
                    this,
                    "Please select expiry date",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        boolean inserted = dbHelper.insertFood(
                name,
                quantity,
                selectedExpiryDate
        );

        if (inserted) {

            Toast.makeText(
                    this,
                    "Food item saved successfully",
                    Toast.LENGTH_SHORT
            ).show();

            etFoodName.setText("");
            etQuantity.setText("");

            selectedExpiryDate = "";
            btnExpiryDate.setText("Select Expiry Date");

            displayFoodItems();

        } else {

            Toast.makeText(
                    this,
                    "Failed to save food item",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void displayFoodItems() {

        foodList.clear();
        foodIds.clear();

        Cursor cursor = dbHelper.getAllFood();

        try {

            while (cursor.moveToNext()) {

                int id = cursor.getInt(
                        cursor.getColumnIndexOrThrow("id")
                );

                String name = cursor.getString(
                        cursor.getColumnIndexOrThrow("name")
                );

                String quantity = cursor.getString(
                        cursor.getColumnIndexOrThrow("quantity")
                );

                String expiryDate = cursor.getString(
                        cursor.getColumnIndexOrThrow("expiry_date")
                );

                String status = getExpiryStatus(expiryDate);

                String item = "Food: " + name +
                        "\nQuantity: " + quantity +
                        "\nExpiry Date: " + expiryDate +
                        "\nStatus: " + status;

                foodIds.add(id);
                foodList.add(item);
            }

        } finally {
            cursor.close();
        }

        adapter.notifyDataSetChanged();

        tvFoodCount.setText("Total items: " + foodList.size());
    }

    private String getExpiryStatus(String expiryDate) {

        SimpleDateFormat sdf =
                new SimpleDateFormat("yyyy-MM-dd", Locale.US);

        sdf.setLenient(false);

        try {

            Date expiry = sdf.parse(expiryDate);

            if (expiry == null) {
                return "Unknown";
            }

            Calendar expiryCalendar = Calendar.getInstance();
            expiryCalendar.setTime(expiry);
            expiryCalendar.set(
                    Calendar.HOUR_OF_DAY, 0
            );
            expiryCalendar.set(
                    Calendar.MINUTE, 0
            );
            expiryCalendar.set(
                    Calendar.SECOND, 0
            );
            expiryCalendar.set(
                    Calendar.MILLISECOND, 0
            );

            Calendar todayCalendar = Calendar.getInstance();
            todayCalendar.set(
                    Calendar.HOUR_OF_DAY, 0
            );
            todayCalendar.set(
                    Calendar.MINUTE, 0
            );
            todayCalendar.set(
                    Calendar.SECOND, 0
            );
            todayCalendar.set(
                    Calendar.MILLISECOND, 0
            );

            long difference = expiryCalendar.getTimeInMillis()
                    - todayCalendar.getTimeInMillis();

            long daysLeft = TimeUnit.MILLISECONDS.toDays(difference);

            if (daysLeft < 0) {
                return "EXPIRED";
            } else if (daysLeft <= 3) {
                return "EXPIRING SOON";
            } else {
                return "FRESH";
            }

        } catch (ParseException e) {
            return "Unknown";
        }
    }
}