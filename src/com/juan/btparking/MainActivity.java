package com.juan.btparking;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.text.Editable;
import android.text.TextWatcher;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends Activity {

    private static final int REQ_PERMS = 100;

    private TextView statusView;
    private TextView pairedView;
    private ListView listView;
    private EditText nameFilterView;
    private int recentDaysFilter = 0;
    private List<LocationStore.Entry> entries = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#0F1115"));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(28, 32, 28, 16);

        TextView title = new TextView(this);
        title.setText("BT Parking");
        title.setTextColor(Color.WHITE);
        title.setTextSize(24f);
        header.addView(title);

        statusView = new TextView(this);
        statusView.setTextColor(Color.parseColor("#AAAAAA"));
        statusView.setPadding(0, 8, 0, 8);
        statusView.setText("Guarda la ubicacion automaticamente cuando se desconecta un dispositivo Bluetooth emparejado.");
        header.addView(statusView);

        Button permBtn = new Button(this);
        permBtn.setText("1) Otorgar permisos");
        permBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { requestPerms(); }
        });
        header.addView(permBtn);

        Button bgBtn = new Button(this);
        bgBtn.setText("2) Ubicacion \"Permitir todo el tiempo\" (ajustes)");
        bgBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { openAppSettings(); }
        });
        header.addView(bgBtn);

        Button testBtn = new Button(this);
        testBtn.setText("Probar ahora (guarda ubicacion de prueba)");
        testBtn.setBackgroundColor(Color.parseColor("#2E7D32"));
        testBtn.setTextColor(Color.WHITE);
        testBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { testSaveNow(); }
        });
        header.addView(testBtn);

        pairedView = new TextView(this);
        pairedView.setTextColor(Color.parseColor("#888888"));
        pairedView.setPadding(0, 16, 0, 0);
        header.addView(pairedView);

        TextView listTitle = new TextView(this);
        listTitle.setText("Ultimas ubicaciones guardadas:");
        listTitle.setTextColor(Color.WHITE);
        listTitle.setTextSize(16f);
        listTitle.setPadding(0, 24, 0, 8);
        header.addView(listTitle);

        nameFilterView = new EditText(this);
        nameFilterView.setHint("Filtrar por nombre Bluetooth");
        nameFilterView.setHintTextColor(Color.parseColor("#C6CDD6"));
        nameFilterView.setTextColor(Color.WHITE);
        nameFilterView.setSingleLine(true);
        nameFilterView.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (listView != null) refreshList();
            }
            @Override public void afterTextChanged(Editable s) { }
        });
        header.addView(nameFilterView);

        final String[] dateRanges = {"Todas las fechas", "Ultimas 24 horas", "Ultimos 7 dias", "Ultimos 30 dias"};
        final int[] dateRangeDays = {0, 1, 7, 30};
        Spinner dateFilterView = new Spinner(this);
        ArrayAdapter<String> dateAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, dateRanges);
        dateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dateFilterView.setAdapter(dateAdapter);
        dateFilterView.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                recentDaysFilter = dateRangeDays[position];
                if (listView != null) refreshList();
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { }
        });
        header.addView(dateFilterView);

        root.addView(header, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        listView = new ListView(this);
        root.addView(listView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(root);
        requestPerms();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshPairedDevices();
        refreshList();
    }

    private void requestPerms() {
        List<String> perms = new ArrayList<>();
        perms.add(Manifest.permission.ACCESS_FINE_LOCATION);
        perms.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        if (Build.VERSION.SDK_INT >= 31) perms.add(Manifest.permission.BLUETOOTH_CONNECT);
        if (Build.VERSION.SDK_INT >= 33) perms.add(Manifest.permission.POST_NOTIFICATIONS);
        List<String> toRequest = new ArrayList<>();
        for (String p : perms) {
            if (checkSelfPermission(p) != PackageManager.PERMISSION_GRANTED) toRequest.add(p);
        }
        if (!toRequest.isEmpty()) {
            requestPermissions(toRequest.toArray(new String[0]), REQ_PERMS);
        } else {
            Toast.makeText(this, "Permisos basicos OK. No olvides el paso 2 (ubicacion en segundo plano).", Toast.LENGTH_LONG).show();
        }
    }

    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + getPackageName()));
        startActivity(intent);
        Toast.makeText(this, "Buscá Permisos > Ubicacion > Permitir todo el tiempo", Toast.LENGTH_LONG).show();
    }

    @SuppressWarnings("MissingPermission")
    private void refreshPairedDevices() {
        try {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if (adapter == null) {
                pairedView.setText("Este dispositivo no tiene Bluetooth.");
                return;
            }
            Set<BluetoothDevice> bonded = adapter.getBondedDevices();
            if (bonded == null || bonded.isEmpty()) {
                pairedView.setText("No hay dispositivos Bluetooth emparejados todavia.");
                return;
            }
            StringBuilder sb = new StringBuilder("Se van a rastrear (emparejados): ");
            boolean first = true;
            for (BluetoothDevice d : bonded) {
                if (!first) sb.append(", ");
                sb.append(d.getName() != null ? d.getName() : d.getAddress());
                first = false;
            }
            pairedView.setText(sb.toString());
        } catch (SecurityException se) {
            pairedView.setText("Falta el permiso de Bluetooth (paso 1).");
        }
    }

    private void testSaveNow() {
        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        Location best = null;
        try {
            for (String p : new String[]{LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER}) {
                if (lm.getProvider(p) == null) continue;
                Location loc = lm.getLastKnownLocation(p);
                if (loc != null && (best == null || loc.getTime() > best.getTime())) best = loc;
            }
        } catch (SecurityException se) {
            Toast.makeText(this, "Falta permiso de ubicacion (paso 1).", Toast.LENGTH_LONG).show();
            return;
        }
        if (best == null) {
            Toast.makeText(this, "Todavia no hay una ubicacion GPS conocida. Abri Google Maps un momento y volve a intentar.", Toast.LENGTH_LONG).show();
            return;
        }
        LocationStore.save(this, "TEST", "PRUEBA MANUAL", best.getLatitude(), best.getLongitude(), System.currentTimeMillis());
        Toast.makeText(this, "Guardado. Mira la lista de abajo.", Toast.LENGTH_SHORT).show();
        refreshList();
    }

    private void refreshList() {
        List<LocationStore.Entry> allEntries = LocationStore.loadAll(this);
        entries = new ArrayList<>();
        String nameQuery = nameFilterView == null ? "" : nameFilterView.getText().toString();
        long now = System.currentTimeMillis();
        for (LocationStore.Entry entry : allEntries) {
            if (HistoryFilter.matches(entry.name, entry.timeMillis, nameQuery, recentDaysFilter, now)) {
                entries.add(entry);
            }
        }
        Collections.sort(entries, new Comparator<LocationStore.Entry>() {
            @Override public int compare(LocationStore.Entry a, LocationStore.Entry b) {
                return Long.compare(b.timeMillis, a.timeMillis);
            }
        });
        List<String> labels = new ArrayList<>();
        SimpleDateFormat fmt = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault());
        for (LocationStore.Entry e : entries) {
            labels.add(e.name + "\n" + fmt.format(new Date(e.timeMillis)) + "  (" + String.format(Locale.US, "%.5f, %.5f", e.lat, e.lon) + ")");
        }
        if (labels.isEmpty()) {
            labels.add(allEntries.isEmpty()
                    ? "(vacio) - desconecta un dispositivo Bluetooth o toca \"Probar ahora\""
                    : "No hay ubicaciones que coincidan con los filtros");
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, labels);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (position >= entries.size()) return;
                LocationStore.Entry e = entries.get(position);
                Uri geo = Uri.parse("geo:" + e.lat + "," + e.lon + "?q=" + e.lat + "," + e.lon + "(" + Uri.encode(e.name) + ")");
                Intent i = new Intent(Intent.ACTION_VIEW, geo);
                try {
                    startActivity(i);
                } catch (Exception ex) {
                    Toast.makeText(MainActivity.this, "No hay app de mapas instalada.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
