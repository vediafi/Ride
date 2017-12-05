package com.indagon.kimppakyyti.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.indagon.kimppakyyti.Constants;
import com.indagon.kimppakyyti.R;
import com.indagon.kimppakyyti.tools.DataManager;
import com.indagon.kimppakyyti.tools.Util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import butterknife.BindView;
import butterknife.ButterKnife;

public class HistoryFragment extends Fragment {
    private OnHistoryFragmentInteractionListener mListener;

    @BindView(R.id.history_container) LinearLayout historyContainer;

    public HistoryFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_history, container, false);

        ButterKnife.bind(this, v);

        try {
            JSONArray rides = this.mListener.getDataManager().getDoneRides();
            String myId = this.mListener.getDataManager().getUsername();

            for (int i = 0; i < rides.length(); i++)  {
                final JSONObject ride = rides.getJSONObject(i);
                final int rideId = ride.getInt(Constants.JSON_FIELD_ID);

                ConstraintLayout item = Util.createAndFillRideElement(ride, myId, inflater);
                item.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mListener.viewRide(rideId, true);
                    }
                });
                historyContainer.addView(item);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return v;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnHistoryFragmentInteractionListener) {
            mListener = (OnHistoryFragmentInteractionListener) context;
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

    @Override
    public void onResume() {
        super.onResume();
        getActivity().setTitle(R.string.menu_option_history);
    }

    public interface OnHistoryFragmentInteractionListener {
        void showText(String text);
        void viewRide(JSONObject ride, boolean joined);
        void viewRide(int rideId, boolean joined);
        DataManager getDataManager();
    }
}
