package com.indagon.kimppakyyti.fragment;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.indagon.kimppakyyti.Constants;
import com.indagon.kimppakyyti.R;
import com.indagon.kimppakyyti.tools.Callback;
import com.indagon.kimppakyyti.tools.CommonRequests;
import com.indagon.kimppakyyti.tools.DataManager;
import com.indagon.kimppakyyti.tools.Util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link PaymentsFragment.OnPaymentsInteractionListener} interface
 * to handle interaction events.
 */
public class PaymentsFragment extends Fragment {
    private OnPaymentsInteractionListener mListener;

    @BindView(R.id.my_debts_container) LinearLayout myDebtsContainer;
    @BindView(R.id.debts_to_me_container) LinearLayout debtsToMeContainer;

    public PaymentsFragment() {
        // Required empty public constructor
    }

    // Update data contained in this fragment if necessary. Called when user navigates to this
    // fragment.
    public void update() {
        try {
            JSONArray payments = this.mListener.getDataManager().getPayments();
            final String myId = this.mListener.getDataManager().getUsername();

            for (int i = 0; i < payments.length(); i++)  {
                JSONObject payment = payments.getJSONObject(i);
                String toUser = payment.getString(Constants.JSON_FIELD_TO_USER);
                String toUserName = payment.optString(Constants.JSON_FIELD_TO_USER_NAME,
                        "Unknown");
                final String fromUser = payment.getString(Constants.JSON_FIELD_FROM_USER);
                final String fromUserName = payment.optString(Constants.JSON_FIELD_FROM_USER_NAME,
                        "Unknown");
                int totalAmount = payment.getInt(Constants.JSON_FIELD_AMOUNT);

                // Dont add payments with zero amount
                if (totalAmount == 0)
                    continue;

                ConstraintLayout item = (ConstraintLayout) this.getLayoutInflater()
                        .inflate(R.layout.element_payment, null, false);

                // Find elements fields
                final ConstraintLayout settleContainer = (ConstraintLayout)
                        item.findViewById(R.id.settle_container);
                final TextView amount = item.findViewById(R.id.amount);
                Button receiveAmountButton = item.findViewById(R.id.receive_amount_button);
                Button settleButton = item.findViewById(R.id.settle_button);
                TextView name = item.findViewById(R.id.name);
                TextView totalAmountField = item.findViewById(R.id.total_amount);

                settleContainer.setVisibility(View.GONE);

                // Set total amount
                double totalAmountDoubleCents = (double) totalAmount;
                final double totalAmountDoubleEuros = totalAmountDoubleCents / 100.0d;
                String totalAmountString = Double.toString(totalAmountDoubleEuros);
                totalAmountField.setText(totalAmountString);

                // Set input value as default to whole debt
                amount.setText(totalAmountString);

                // I owe toUser money
                if (!toUser.equals(myId) && fromUser.equals(myId)) {
                    name.setText(toUserName);
                    settleButton.setVisibility(View.INVISIBLE);

                    this.myDebtsContainer.addView(item);
                }
                // fromUser owes me money
                else if (toUser.equals(myId) && !fromUser.equals(myId)) {
                    name.setText(fromUserName);
                    // When user clicks settle button show area where user can receive money
                    settleButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (settleContainer.getVisibility() == View.VISIBLE) {
                                settleContainer.setVisibility(View.GONE);
                            } else {
                                settleContainer.setVisibility(View.VISIBLE);
                            }
                        }
                    });

                    // When user clicks receive money validate input and send request
                    receiveAmountButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                        // Validate amount
                        Double amountDouble = Double.parseDouble(amount.getText().toString());
                        final String amountString = Double.toString(Math.abs(amountDouble));
                        int integerPlaces = amountString.indexOf('.');
                        int decimalPlaces = amountString.length() - integerPlaces - 1;

                        // Check that user has not defined amount in higher definition
                        // than two decimals (cents)
                        if (decimalPlaces > 2) {
                            PaymentsFragment.this.mListener.showText(getString(
                                    R.string.error_amount_precision));
                            return;
                        }
                        // Check that inputted amount isn't greater than the amount that is owed
                        if (amountDouble > totalAmountDoubleEuros) {
                            PaymentsFragment.this.mListener.showText(getString(
                                    R.string.error_receive_too_much));
                            return;
                        }
                        // Check that inputted value is positive
                        if (amountDouble <= 0.0) {
                            PaymentsFragment.this.mListener.showText(getString(
                                    R.string.error_receive_zero));
                            return;
                        }

                        Double amountDoubleCents = amountDouble * 100.0d;
                        final int amountInt = amountDoubleCents.intValue();
                        String payConfifmationMessage = String.format(
                                getString(R.string.confirm_payment),
                                amountString, fromUserName);

                        Util.createConfirmationDialog(PaymentsFragment.this.getContext(),
                                payConfifmationMessage,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    // Send request
                                    CommonRequests.receivePayment(PaymentsFragment.this.getContext(),
                                            fromUser, amountInt, new Callback<JSONObject>() {
                                        @Override
                                        public void callback(JSONObject result) {
                                            if (result != null) {
                                                mListener.showText(String.format(getString(
                                                        R.string.received_payment), amountString));
                                                Log.e("OASD", result.toString());
                                                try {
                                                    JSONObject debt = result.getJSONObject(
                                                            Constants.JSON_FIELD_DEBT);
                                                    mListener.getDataManager()
                                                            .addOrUpdatePayment(debt);
                                                } catch (JSONException e) {
                                                    e.printStackTrace();
                                                }
                                                mListener.switchTab(Constants.Tab.PAYMENTS, false);
                                            } else {
                                                mListener.showText(getString(
                                                        R.string.receiving_payment_failed));
                                            }
                                        }
                                    });
                                }
                        });

                        }
                    });
                    this.debtsToMeContainer.addView(item);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().setTitle(R.string.menu_option_payments);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_payments, container, false);

        ButterKnife.bind(this, v);

        this.update();

        return v;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnPaymentsInteractionListener) {
            mListener = (OnPaymentsInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnPaymentsInteractionListener {
        void showText(String text);
        DataManager getDataManager();
        void switchTab(Constants.Tab tab, boolean addToBackStack);
    }
}