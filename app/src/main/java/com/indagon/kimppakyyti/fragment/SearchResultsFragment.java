package com.indagon.kimppakyyti.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.Fragment;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.indagon.kimppakyyti.Constants;
import com.indagon.kimppakyyti.R;
import com.indagon.kimppakyyti.tools.Util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link SearchResultsFragment.OnSearchResultsFragmentInteractionListener} interface
 * to handle interaction events.
 */
public class SearchResultsFragment extends Fragment {
    private JSONArray searchResults;
    private OnSearchResultsFragmentInteractionListener mListener;

    @BindView(R.id.search_result_container) LinearLayout searchResultContainer;

    public SearchResultsFragment() {
        // Required empty public constructor
    }

    // Store new search results and fill ui when ui is inflated
    public void setResults(JSONArray results)
    {
        this.searchResults = results;
    }

    public void update() {
        // Clear search results
        this.searchResultContainer.removeAllViews();

        // Add search results to UI
        for (int i = 0; i < this.searchResults.length(); i++)
        {
            try {
                final JSONObject ride = searchResults.getJSONObject(i);
                final JSONObject route = ride.getJSONObject(Constants.JSON_FIELD_ROUTE);
                JSONArray stops = route.getJSONArray(Constants.JSON_FIELD_STOPS);
                final JSONObject startLocation =
                        route.getJSONObject(Constants.JSON_FIELD_START_LOCATION);
                final JSONObject endLocation = stops.getJSONObject(stops.length() - 1);
                long time = route.getLong(Constants.JSON_FIELD_TIME);
                double distance = route.getDouble(Constants.JSON_FIELD_DISTANCE);
                String startLocationString = startLocation.getString(Constants.JSON_FIELD_ADDRESS);
                String endLocationString = endLocation.getString(Constants.JSON_FIELD_ADDRESS);

                ConstraintLayout item = (ConstraintLayout) this.getActivity()
                        .getLayoutInflater().inflate(R.layout.element_search_result, null, false);

                TextView timeText = item.findViewById(R.id.time);
                TextView distanceText = item.findViewById(R.id.distance);
                TextView start = item.findViewById(R.id.start);
                TextView end = item.findViewById(R.id.end);

                timeText.setText(DateUtils.formatElapsedTime(time));
                distanceText.setText(Util.distanceToString(distance));
                start.setText(Util.shortenString(startLocationString, 24));
                end.setText(Util.shortenString(endLocationString, 24));

                item.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mListener.viewRide(ride, false);
                    }
                });

                this.searchResultContainer.addView(item);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_search_results, container, false);
        ButterKnife.bind(this, v);
        this.update();

        return v;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnSearchResultsFragmentInteractionListener) {
            mListener = (OnSearchResultsFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().setTitle(getString(R.string.search_results));
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnSearchResultsFragmentInteractionListener {
        void showText(String text);
        void viewRide(JSONObject ride, boolean joined);
    }
}
