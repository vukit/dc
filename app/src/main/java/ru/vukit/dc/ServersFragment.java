package ru.vukit.dc;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.ListFragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import ru.vukit.dc.database.DBDriver;
import ru.vukit.dc.servres.DCServer;

@Keep
public class ServersFragment extends ListFragment implements FragmentInterface {

    final Handler mainLooperHandler = new Handler(Looper.getMainLooper());
    final ServersFragmentState state = ServersFragmentState.getInstance();
    final DBDriver dbDriver = DBDriver.getInstance();

    ServersAdapter serversAdapter;
    FloatingActionButton fabAddServer;
    private GestureDetector mGestureDetector;

    @Override
    public void onResume() {
        super.onResume();
        dbDriver.connectController(this);
        ActionBar actionBar = ((AppCompatActivity) requireActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(false);
            actionBar.setTitle(getResources().getString(R.string.action_servers));
        }
        serversAdapter = new ServersAdapter(getActivity(), R.layout.server_list_item, dbDriver);
        setListAdapter(serversAdapter);
        mainLooperHandler.post(new Runnable() {
            @Override
            public void run() {
                if (serversAdapter != null) {
                    requireActivity().invalidateOptionsMenu();
                    serversAdapter.notifyDataSetChanged();
                    mainLooperHandler.postDelayed(this, DCServer.TIMEOUT);
                }
            }
        });
    }

    @Override
    public void onPause() {
        dbDriver.disconnectController();
        serversAdapter = null;
        super.onPause();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.servers, container, false);
        fabAddServer = view.findViewById(R.id.server_fab_add);
        fabAddServer.setOnClickListener(onClickListenerFabAddServer);
        mGestureDetector = new GestureDetector(getActivity(), new MyGestureDetector());
        return view;
    }

    @Override
    public void updateView() {
        requireActivity().invalidateOptionsMenu();
        serversAdapter.notifyDataSetChanged();
    }

    private class ServersAdapter extends BaseAdapter {

        private final Context context;
        private final int resource;
        private final DBDriver dbDriver;
        private int count;
        private Cursor rowIds;
        int currentPosition;

        ServersAdapter(Context context, int resource, DBDriver dbDriver) {
            this.context = context;
            this.resource = resource;
            this.dbDriver = dbDriver;
            closeCursor();
            this.rowIds = getRowIds();
            this.count = this.rowIds.getCount();
        }

        @Override
        public int getCount() {
            return count;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View row = convertView;
            if (inflater != null) {
                if (row == null) row = inflater.inflate(resource, parent, false);
                TextView serverItem = row.findViewById(R.id.server_list_item);
                Cursor cursor = getServer(position);
                CharSequence server_info;
                String name = cursor.getString(cursor.getColumnIndexOrThrow(DBDriver.DataBaseContract.Servers.NAME));
                SpannableString server_parameter = new SpannableString("\n" + name + "\n");
                server_parameter.setSpan(new ForegroundColorSpan(0xFF3F51B5), 1, server_parameter.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                server_parameter.setSpan(new TypefaceSpan("monospace"), 1, server_parameter.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                server_info = TextUtils.concat(server_parameter);
                String url = cursor.getString(cursor.getColumnIndexOrThrow(DBDriver.DataBaseContract.Servers.URL));
                server_parameter = new SpannableString(getString(R.string.server_url) + ": " + url + "\n");
                server_parameter.setSpan(new StyleSpan(Typeface.BOLD), 0, getString(R.string.server_url).length() + 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                server_parameter.setSpan(new TypefaceSpan("monospace"), getString(R.string.server_url).length() + 2, server_parameter.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                server_info = TextUtils.concat(server_info, server_parameter);
                String protocol = cursor.getString(cursor.getColumnIndexOrThrow(DBDriver.DataBaseContract.Servers.PROTOCOL));
                if (!protocol.isEmpty()) {
                    server_parameter = new SpannableString(getString(R.string.server_protocol) + ": " + protocol + "\n");
                    server_parameter.setSpan(new StyleSpan(Typeface.BOLD), 0, getString(R.string.server_protocol).length() + 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    server_parameter.setSpan(new TypefaceSpan("monospace"), getString(R.string.server_protocol).length() + 2, server_parameter.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    server_info = TextUtils.concat(server_info, server_parameter);
                }
                String status = getResources().getString(R.string.server_disable);
                if (cursor.getString(cursor.getColumnIndexOrThrow(DBDriver.DataBaseContract.Servers.ENABLE)).equals("1")) {
                    status = getStatus(position);
                }
                server_parameter = new SpannableString(getString(R.string.server_status) + ": " + status + "\n");
                server_parameter.setSpan(new StyleSpan(Typeface.BOLD), 0, getString(R.string.server_status).length() + 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                server_parameter.setSpan(new TypefaceSpan("monospace"), getString(R.string.server_status).length() + 2, server_parameter.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                server_info = TextUtils.concat(server_info, server_parameter);
                serverItem.setText(server_info);
                cursor.close();
                row.setOnTouchListener((view, motionEvent) -> {
                    currentPosition = position;
                    mGestureDetector.onTouchEvent(motionEvent);
                    return view.performClick();
                });
            }
            return row;
        }

        @Override
        public Object getItem(int position) {
            if (rowIds.moveToPosition(position)) {
                long rowId = rowIds.getLong(0);
                return getRowById(rowId);
            } else return null;
        }

        @Override
        public long getItemId(int position) {
            if (rowIds.moveToPosition(position)) return rowIds.getLong(0);
            else return 0;
        }

        Cursor getRowIds() {
            return dbDriver.rawQuery("SELECT * FROM servers");
        }

        Cursor getRowById(long rowId) {
            return dbDriver.rawQuery("SELECT * FROM " + DBDriver.DataBaseContract.Servers.TABLE_NAME + " WHERE _ID = " + rowId);
        }

        String getStatus(int position) {
            return state.serverStatus.getOrDefault(String.valueOf(getItemId(position)), DCServer.STATUS_UNKNOWN);
        }

        Cursor getServer(int position) {
            rowIds.moveToPosition(position);
            long rowId = rowIds.getLong(0);
            Cursor cursor = getRowById(rowId);
            cursor.moveToFirst();
            return cursor;
        }

        @Override
        public void notifyDataSetChanged() {
            super.notifyDataSetChanged();
            closeCursor();
            this.rowIds = getRowIds();
            this.count = this.rowIds.getCount();
        }

        private void closeCursor() {
            if (this.rowIds != null && !this.rowIds.isClosed()) this.rowIds.close();
        }

        @Override
        protected void finalize() throws Throwable {
            closeCursor();
            super.finalize();
        }
    }

    class MyGestureDetector implements GestureDetector.OnDoubleTapListener, GestureDetector.OnGestureListener {
        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            editServer(serversAdapter.currentPosition);
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            return false;
        }

        @Override
        public boolean onDoubleTapEvent(MotionEvent e) {
            return false;
        }

        @Override
        public boolean onDown(MotionEvent e) {
            return false;
        }

        @Override
        public void onShowPress(MotionEvent e) {
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            return false;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            return false;
        }

        @Override
        public void onLongPress(MotionEvent e) {
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            return false;
        }
    }

    final View.OnClickListener onClickListenerFabAddServer = v -> {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        View view = View.inflate(getActivity(), R.layout.add_new_server, null);
        final EditText name = view.findViewById(R.id.server_name);
        final EditText url = view.findViewById(R.id.server_url);
        final EditText username = view.findViewById(R.id.server_username);
        final EditText password = view.findViewById(R.id.server_password);
        final EditText protocol = view.findViewById(R.id.server_protocol);
        final CheckBox enable = view.findViewById(R.id.server_enable);
        builder.setView(view);
        builder.setPositiveButton(R.string.save, (dialog, id) -> {
            if (!name.getText().toString().trim().isEmpty() && !url.getText().toString().trim().isEmpty()) {
                DBDriver.getInstance().createServer(
                        name.getText().toString().trim(),
                        url.getText().toString().trim(),
                        username.getText().toString().trim(),
                        password.getText().toString().trim(),
                        protocol.getText().toString().trim(),
                        (enable.isChecked() ? "1" : "0")
                );
            }
        });
        builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> {
        });
        builder.setTitle(R.string.new_server_dialog_title);
        builder.create();
        builder.show();
    };

    public void editServer(int position) {
        Cursor cursor = serversAdapter.getServer(position);
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        View dialogView = View.inflate(getActivity(), R.layout.add_new_server, null);
        final EditText name = dialogView.findViewById(R.id.server_name);
        final EditText url = dialogView.findViewById(R.id.server_url);
        final EditText username = dialogView.findViewById(R.id.server_username);
        final EditText password = dialogView.findViewById(R.id.server_password);
        final EditText protocol = dialogView.findViewById(R.id.server_protocol);
        final CheckBox enable = dialogView.findViewById(R.id.server_enable);
        final String serverId = cursor.getString(cursor.getColumnIndexOrThrow(DBDriver.DataBaseContract.Servers._ID));
        name.setText(cursor.getString(cursor.getColumnIndexOrThrow(DBDriver.DataBaseContract.Servers.NAME)));
        url.setText(cursor.getString(cursor.getColumnIndexOrThrow(DBDriver.DataBaseContract.Servers.URL)));
        username.setText(cursor.getString(cursor.getColumnIndexOrThrow(DBDriver.DataBaseContract.Servers.USERNAME)));
        password.setText(cursor.getString(cursor.getColumnIndexOrThrow(DBDriver.DataBaseContract.Servers.PASSWORD)));
        protocol.setText(cursor.getString(cursor.getColumnIndexOrThrow(DBDriver.DataBaseContract.Servers.PROTOCOL)));
        enable.setChecked(cursor.getString(cursor.getColumnIndexOrThrow(DBDriver.DataBaseContract.Servers.ENABLE)).equals("1"));
        builder.setView(dialogView);
        builder.setPositiveButton(R.string.save, (dialog, id) -> DBDriver.getInstance().updateServer(
                serverId,
                name.getText().toString().trim(),
                url.getText().toString().trim(),
                username.getText().toString().trim(),
                password.getText().toString().trim(),
                protocol.getText().toString().trim(),
                (enable.isChecked() ? "1" : "0")
        ));
        builder.setNegativeButton(R.string.delete, (dialog, which) -> DBDriver.getInstance().deleteServer(serverId));
        builder.setTitle(R.string.edit_server_dialog_title);
        builder.create();
        builder.show();
        cursor.close();
    }
}
