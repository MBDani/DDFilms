package com.merino.ddfilms.ui.fragment;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.merino.ddfilms.R;

public class CreateListDialogFragment extends DialogFragment {

    private EditText listNameInput;
    private OnListCreatedListener listener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_create_list, container, false);

        listNameInput = view.findViewById(R.id.list_name_input);
        Button createButton = view.findViewById(R.id.create_button);
        ImageView closeButton = view.findViewById(R.id.close_button);

        createButton.setOnClickListener(v -> {
            String listName = listNameInput.getText().toString().trim();
            if (TextUtils.isEmpty(listName)) {
                listNameInput.setError("El nombre no puede estar vacío");
            } else {
                if (listener != null) {
                    listener.onListCreated(listName);
                }
                dismiss();
            }
        });

        closeButton.setOnClickListener(v -> dismiss());

        return view;
    }

    public void setOnListCreatedListener(OnListCreatedListener listener) {
        this.listener = listener;
    }

    public interface OnListCreatedListener {
        void onListCreated(String listName);
    }
}