package net.xzos.UpgradeAll.ui.viewmodels.log;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import net.xzos.UpgradeAll.R;

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
        pageViewModel = ViewModelProviders.of(this).get(PageViewModel.class);
        String[] logObjectTag = {null, null};
        if (getArguments() != null) {
            logObjectTag = getArguments().getStringArray(ARG_SECTION_NUMBER);
        }
        pageViewModel.setLogObjectTag(logObjectTag);
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_log, container, false);
        final ListView logListView = root.findViewById(R.id.log_list);
        pageViewModel.getLogList().observe(this, logArray -> {
            @SuppressWarnings("unchecked") ArrayAdapter<String> adapter = new ArrayAdapter<String>(root.getContext(), android.R.layout.simple_expandable_list_item_1, logArray);
            logListView.setAdapter(adapter);
            // 点击复制到粘贴板
            logListView.setOnItemClickListener((parent, view, position, id) -> {
                ClipboardManager cm = (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData mClipData = ClipData.newPlainText("Label", (CharSequence) logArray.get(position));
                cm.setPrimaryClip(mClipData);
                Toast.makeText(mContext, "已复制到粘贴板", Toast.LENGTH_SHORT).show();
            });
        });
        return root;
    }
}