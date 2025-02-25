/*
 * (C) Copyright Syd Logan 2017
 * (C) Copyright Thousand Smiles Foundation 2017
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.thousandsmiles.tschartsregister;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.fragment.app.DialogFragment;
import androidx.appcompat.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.thousandsmiles.tscharts_lib.CommonSessionSingleton;
import org.thousandsmiles.tscharts_lib.MedicationsModel;
import org.thousandsmiles.tscharts_lib.MedicationsModelList;
import org.thousandsmiles.tscharts_lib.MedicationsREST;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MedicationsListDialogFragment extends DialogFragment {

    private int m_patientId;
    private View m_view;
    private MedicationsModelList m_list = MedicationsModelList.getInstance();
    MedicationsAdapter m_adapter;
    private TextView m_textView;
    private SessionSingleton m_sess = SessionSingleton.getInstance();

    public void setPatientId(int id) {
        m_patientId = id;
    }

    /**
     * Record the resource ID of the medications field this dialog pertains to.
     *
     * @param view EditText view being edited
     */

    public void setTextField(TextView view) {
        m_textView = view;
    }

    private String getTextField()
    {
        String ret = new String();

        if (m_textView != null) {
            ret = m_textView.getText().toString();
        }
        return ret;
    }

    private void setTextField(String str)
    {
        if (m_textView != null) {
            m_textView.setText(str);
        }
    }

    private ArrayList<String> getCheckedMedicinesFromUI()
    {
        return m_adapter.getCheckedItems();
    }

    private void removeMedicinesFromUI(ArrayList<String> meds)
    {
        ListView listView = (ListView) m_view.findViewById(R.id.medications_list);
        for (int i = 0; i < meds.size(); i++) {
            for (int j = 0; j < listView.getCount(); j++) {
                View v = listView.getChildAt(j);
                if (v != null) {
                    TextView t = (TextView) v.findViewById(R.id.label);
                    if (t.getText().toString().equals(meds.get(i))) {
                        listView.removeViewInLayout(v);
                    }
                }
            }
        }
        m_adapter.removeMedicines(meds);
        if (listView.getCount() == 0) {
            View v = m_view.findViewById(R.id.medications_list);
            v.setVisibility(View.GONE);
            v = m_view.findViewById(R.id.remove_med_button);
            v.setVisibility(View.GONE);
        }
    }

    private void addMedicineToUI(String med)
    {
        if (med.length() > 0) {
            MedicationsModel medication = new MedicationsModel(med, false);
            m_adapter.add(medication);
            m_adapter.notifyDataSetChanged();
            View v = m_view.findViewById(R.id.medications_list);
            v.setVisibility(View.VISIBLE);
            v = m_view.findViewById(R.id.remove_med_button);
            v.setVisibility(View.VISIBLE);
        } else {
            Toast.makeText(getActivity(), R.string.msg_enter_a_medication, Toast.LENGTH_SHORT).show();
        }
    }

    private String itemsToCSV()
    {
        ArrayList<String> items = m_adapter.getAllItems();

        String csv = new String();

        for (int i = 0; i < items.size(); i++) {
            csv += items.get(i);
            if (i < items.size() - 1 && items.size() > 1) {
                csv += ", ";
            }
        }
        return csv;
    }

    public void CSVToItems(String str)
    {
        ArrayList<String> items = new ArrayList<String>();

        List<String> tmp = Arrays.asList(str.split("\\s*,\\s*"));

        if (tmp != null) {
            for (int i = 0; i < tmp.size(); i++) {
                if (tmp.get(i).equals("") == false) {
                    items.add(tmp.get(i));
                }
            }
        }
        m_list.setModelData(items);
    }

    public class GetMedicationsList extends AsyncTask<Object, Object, Object> {
        @Override
        protected String doInBackground(Object... params) {
            getMedicationsList();
            return "";
        }

        private void getMedicationsList() {
            final MedicationsREST medsREST = new MedicationsREST(m_sess.getContext());
            Object lock = medsREST.getMedicationsList();

            synchronized (lock) {
                // we loop here in case of race conditions or spurious interrupts
                while (true) {
                    try {
                        lock.wait();
                        break;
                    } catch (InterruptedException e) {
                        continue;
                    }
                }
            }

            int status = medsREST.getStatus();
            if (status == 200) {
                getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        AutoCompleteTextView textView = (AutoCompleteTextView) m_view.findViewById(R.id.medsautocomplete);
                        String[] MultipleTextStringValue = CommonSessionSingleton.getInstance().getMedicationsListStringArray();
                        ArrayAdapter<String> medNames = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, MultipleTextStringValue);
                        textView.setAdapter(medNames);
                        textView.setThreshold(2);
                    }
                });
            } else {
                getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(getActivity(), R.string.msg_unable_to_get_medications_list, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    }

    private void configMedicationsAutocomplete()
    {
        // get medications list from backend

        AsyncTask task = new GetMedicationsList();
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Object) null);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        m_view = inflater.inflate(R.layout.medications_list_dialog, null);

        ListView listView = (ListView) m_view.findViewById(R.id.medications_list);

        String str = getTextField();

        CSVToItems(str);

        m_adapter = new MedicationsAdapter(getActivity(), m_list.getModel());
        listView.setAdapter(m_adapter);

        View button_item = m_view.findViewById(R.id.add_med_button);
        button_item.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                AutoCompleteTextView textView = (AutoCompleteTextView) m_view.findViewById(R.id.medsautocomplete);
                String med = textView.getText().toString();
                addMedicineToUI(med);
                textView.setText("");

                InputMethodManager imm = (InputMethodManager)getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(m_view.getWindowToken(), 0);
            }
        });
        button_item = m_view.findViewById(R.id.remove_med_button);
        button_item.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                /* iterate the list for checked items */

                ArrayList checkedItems = getCheckedMedicinesFromUI();

                /* remove from adapter */

                if (checkedItems.size() != 0) {
                    removeMedicinesFromUI(checkedItems);
                }
            }
        });

        builder.setView(m_view)
                // Add action buttons
                .setPositiveButton(R.string.select_medications_yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        /* get all medicines in the list */

                        ArrayList<String> items = m_adapter.getAllItems();

                        String csv = itemsToCSV();
                        setTextField(csv);

                        dialog.dismiss();
                    }
                })
                .setNegativeButton(R.string.select_medications_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });
        Dialog ret = builder.create();
        ret.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        ret.setTitle(R.string.title_edit_medications_dialog);
        configMedicationsAutocomplete();
        if (listView.getCount() == 0) {
            View v = m_view.findViewById(R.id.medications_list);
            v.setVisibility(View.GONE);
            v = m_view.findViewById(R.id.remove_med_button);
            v.setVisibility(View.GONE);
        }
        return ret;
    }
}