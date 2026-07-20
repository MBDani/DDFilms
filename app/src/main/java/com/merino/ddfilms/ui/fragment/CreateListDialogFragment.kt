package com.merino.ddfilms.ui.fragment

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import androidx.fragment.app.DialogFragment
import com.merino.ddfilms.R

class CreateListDialogFragment : DialogFragment() {

    private var listNameInput: EditText? = null
    private var listener: OnListCreatedListener? = null

    fun interface OnListCreatedListener {
        fun onListCreated(listName: String)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_create_list, container, false)

        listNameInput = view.findViewById(R.id.list_name_input)
        val createButton = view.findViewById<Button>(R.id.create_button)
        val closeButton = view.findViewById<ImageView>(R.id.close_button)

        createButton.setOnClickListener {
            val listName = listNameInput?.text?.toString()?.trim() ?: ""
            if (TextUtils.isEmpty(listName)) {
                listNameInput?.error = getString(R.string.list_name_cannot_be_empty)
            } else {
                listener?.onListCreated(listName)
                dismiss()
            }
        }

        closeButton.setOnClickListener { dismiss() }

        return view
    }

    fun setOnListCreatedListener(listener: OnListCreatedListener?) {
        this.listener = listener
    }
}
