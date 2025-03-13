package com.dti.lofo.ui.Lost;

import static android.app.Activity.RESULT_OK;
import static android.content.ContentValues.TAG;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import com.dti.lofo.Utility;
import com.dti.lofo.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class LostItemsFragment extends DialogFragment {
    private TextView dateEdit;
    private TextView timeEdit;
    private Spinner categorySpinner;
    private EditText description;
    private EditText location;
    private int mYear, mMonth, mDay, mHour, mMinute;
    private String date = null;
    private String time = null;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return super.onCreateDialog(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_lost_items, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        description = view.findViewById(R.id.description);
        location = view.findViewById(R.id.location);
        dateEdit = view.findViewById(R.id.selectedDateEditText);
        timeEdit = view.findViewById(R.id.selectedTimeEditText);

        view.findViewById(R.id.datePickerButton).setOnClickListener(v -> showDatePicker());
        view.findViewById(R.id.timePickerButton).setOnClickListener(v -> showTimePicker());

        categorySpinner = view.findViewById(R.id.categorySpinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(requireContext(),
                R.array.categories_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(adapter);

        final String[] selectedCategory = new String[1];
        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedCategory[0] = categorySpinner.getItemAtPosition(position).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        Button submitButton = view.findViewById(R.id.submit_button);
        submitButton.setOnClickListener(v -> {
            EditText item = view.findViewById(R.id.item_name_edittext);
            String itemName = item.getText().toString();

            if (itemName.isEmpty()) {
                Utility.showToast(getContext(), "Name cannot be empty");
                return;
            }

            if (selectedCategory[0] == null) {
                Utility.showToast(getContext(), "Please select a category");
                return;
            }

            if (date == null) {
                showDatePicker();
                return;
            }

            if (time == null) {
                showTimePicker();
                return;
            }

            String loc = location.getText().toString();
            if (loc.isEmpty()) {
                Utility.showToast(getContext(), "Please provide location");
                return;
            }

            String desc = description.getText().toString();
            if (desc.isEmpty()) {
                Utility.showToast(getContext(), "Please add description");
                return;
            }

            LostItems lostItem = new LostItems();
            lostItem.setItemName(itemName);
            lostItem.setCategory(selectedCategory[0]);
            lostItem.setDateLost(date);
            lostItem.setTimeLost(time);
            lostItem.setLocation(location.getText().toString());
            lostItem.setDescription(description.getText().toString());

            FirebaseAuth mAuth = FirebaseAuth.getInstance();
            FirebaseUser currentUser = mAuth.getCurrentUser();
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            if (currentUser != null) {
                String userEmail = currentUser.getEmail();
                String userID = currentUser.getUid();

                db.collection("users")
                        .whereEqualTo("email", userEmail)
                        .get()
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    String userName = document.getString("name");
                                    String userPhone = document.getString("phone");
                                    lostItem.setOwnerName(userName);
                                    lostItem.setPhnum(Long.valueOf(userPhone));
                                    lostItem.setEmail(userEmail);
                                    lostItem.setUserId(userID);
                                }
                            } else {
                                Log.d(TAG, "Error getting documents: ", task.getException());
                            }
                        });
            }

            saveItemToFirebase(lostItem);
        });
    }

    private void saveItemToFirebase(LostItems item) {
        try {
            Utility.getCollectionReferrenceForItems2().document()
                    .set(item)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Utility.showToast(getContext(), "Item added successfully");
                            dismiss();
                        } else {
                            Utility.showToast(getContext(), "Failed to add item");
                            dismiss();
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
            Utility.showToast(getContext(), "An error occurred while saving data");
        }
    }

    private void showDatePicker() {
        final Calendar c = Calendar.getInstance();
        mYear = c.get(Calendar.YEAR);
        mMonth = c.get(Calendar.MONTH);
        mDay = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(),
                (view, year, monthOfYear, dayOfMonth) -> {
                    mYear = year;
                    mMonth = monthOfYear;
                    mDay = dayOfMonth;
                    updateDateButton();
                }, mYear, mMonth, mDay);
        datePickerDialog.show();
    }

    private void showTimePicker() {
        final Calendar c = Calendar.getInstance();
        mHour = c.get(Calendar.HOUR_OF_DAY);
        mMinute = c.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(requireContext(),
                (view, hourOfDay, minute) -> {
                    mHour = hourOfDay;
                    mMinute = minute;
                    updateTimeButton();
                }, mHour, mMinute, false);
        timePickerDialog.show();
    }

    private void updateDateButton() {
        date = mDay + "/" + (mMonth + 1) + "/" + mYear;
        dateEdit.setText(date);
    }

    private void updateTimeButton() {
        String AM_PM = (mHour < 12) ? "AM" : "PM";
        int hour = mHour % 12;
        hour = (hour == 0) ? 12 : hour;
        time = hour + ":" + mMinute + " " + AM_PM;
        timeEdit.setText(time);
    }
}
