package com.dti.lofo.ui.Found;

import static android.app.Activity.RESULT_OK;
import static android.content.ContentValues.TAG;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Intent;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.dti.lofo.Utility;

import com.google.firebase.firestore.DocumentReference;
import com.dti.lofo.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class FoundItemsFragment extends DialogFragment {
    private ImageButton datePickerButton;
    private TextView dateEdit;
    private Spinner categorySpinner;
    private EditText description;
    private EditText location;
    private String date = null;
    private int mYear, mMonth, mDay, mHour, mMinute;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return super.onCreateDialog(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_found_items, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        description = view.findViewById(R.id.description);
        datePickerButton = view.findViewById(R.id.datePickerButton);
        datePickerButton.setOnClickListener(v -> showDatePicker());
        dateEdit = view.findViewById(R.id.selectedDateEditText);
        location = view.findViewById(R.id.location);

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
                // Handle the case when nothing is selected
            }
        });

        Button submitButton = view.findViewById(R.id.submit_button);

        submitButton.setOnClickListener(v -> {
            EditText item = view.findViewById(R.id.item_name_edittext);
            String itemName = item.getText().toString();

            // Validation
            if (itemName.isEmpty()) {
                Utility.showToast(getContext(), "Name cannot be empty");
                return;
            }

            // Check if category is selected
            if (selectedCategory[0] == null) {
                Utility.showToast(getContext(), "Please select a category");
                return;
            }

            if (date == null) {
                showDatePicker();
                return;
            }

            // Check that location is not empty
            String loc = location.getText().toString();
            if (loc.isEmpty()) {
                Utility.showToast(getContext(), "Please provide location");
                return;
            }

            // Check that description is not empty
            String desc = description.getText().toString();
            if (desc.isEmpty()) {
                Utility.showToast(getContext(), "Please add description");
                return;
            }

            FoundItems FoundItem = new FoundItems();
            FoundItem.setItemName(itemName);
            FoundItem.setCategory(selectedCategory[0]);
            FoundItem.setDateFound(date);
            FoundItem.setLocation(location.getText().toString());
            FoundItem.setDescription(description.getText().toString());

            FirebaseAuth mAuth = FirebaseAuth.getInstance();
            FirebaseUser currentUser = mAuth.getCurrentUser();
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            if (currentUser != null) {
                String userEmail = currentUser.getEmail();
                String userID = currentUser.getUid();

                // Query the user collection for the current user's details based on their email
                db.collection("users")
                        .whereEqualTo("email", userEmail)
                        .get()
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    // Retrieve the user's name and phone number from the document
                                    String userName = document.getString("name");
                                    String userPhone = document.getString("phone");
                                    FoundItem.setfinderName(userName);
                                    FoundItem.setPhnum(userPhone);
                                    FoundItem.setEmail(userEmail);
                                    FoundItem.setfinderId(userID);
                                }
                            } else {
                                Log.d(TAG, "Error getting documents: ", task.getException());
                            }
                        });
            }
            saveItemToFirebase(FoundItem);
        });
    }

    private void saveItemToFirebase(FoundItems item) {
        try {
            DocumentReference documentReference = Utility.getCollectionReferrenceForFound().document();
            documentReference.set(item).addOnCompleteListener(task -> {
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
        updateDateButton();
    }

    private String updateDateButton() {
        String date = mDay + "/" + (mMonth + 1) + "/" + mYear;
        dateEdit.setText(date);
        this.date = date;
        return date;
    }
}
