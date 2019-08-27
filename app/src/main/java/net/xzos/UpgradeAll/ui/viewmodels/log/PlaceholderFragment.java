package net.xzos.UpgradeAll.ui.viewmodels.log;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.xzos.UpgradeAll.R;
import net.xzos.UpgradeAll.ui.viewmodels.adapters.LogItemAdapter;

/**
 * A placeholder fragment containing a simple view.
 */
public class PlaceholderFragment extends Fragment {

    private static final String ARG_SECTION_NUMBER = "LogObjectTag";

    private Context mContext;

    private PageViewModel pageViewModel;

    static PlaceholderFragment newInstance(@NonNull String[] logObjectTag) {
        PlaceholderFragment fragment = new PlaceholderFragment();
        Bundle bundle = new Bundle();
        bundle.putStringArray(ARG_SECTION_NUMBER, logObjectTag);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getContext();
        pageViewModel = new ViewModelProvider(this).get(PageViewModel.class);
        String[] logObjectTag = {null, null};
        if (getArguments() != null) {
            logObjectTag = getArguments().getStringArray(ARG_SECTION_NUMBER);
        }
        pageViewModel.setLogObjectTag(logObjectTag);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_log, container, false);
        final RecyclerView logListView = root.findViewById(R.id.log_list);
        GridLayoutManager layoutManager = new GridLayoutManager(mContext, 1);
        logListView.setLayoutManager(layoutManager);
        LogItemAdapter adapter = new LogItemAdapter(pageViewModel.getLogList(), this);
        logListView.setAdapter(adapter);
        return root;
    }
}