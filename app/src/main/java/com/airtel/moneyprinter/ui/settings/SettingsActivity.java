package com.airtel.moneyprinter.ui.settings;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import com.airtel.moneyprinter.R;
import com.airtel.moneyprinter.databinding.ActivitySettingsBinding;

/**
 * Activité des paramètres de l'application.
 */
public class SettingsActivity extends AppCompatActivity {

    private ActivitySettingsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Paramètres");
        }

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings_container, new SettingsFragment())
                    .commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Fragment des préférences.
     */
    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences, rootKey);

            // Port série
            ListPreference portPref = findPreference("printer_port");
            if (portPref != null) {
                portPref.setEntries(new String[]{
                        "Auto (détection)", "/dev/ttyS1", "/dev/ttyS0", "/dev/ttyUSB0"
                });
                portPref.setEntryValues(new String[]{
                        "auto", "/dev/ttyS1", "/dev/ttyS0", "/dev/ttyUSB0"
                });
                if (portPref.getValue() == null) portPref.setValue("auto");
            }

            // Sender SMS
            androidx.preference.EditTextPreference senderPref = findPreference("sms_sender");
            if (senderPref != null && senderPref.getText() == null) {
                senderPref.setText("161");
            }
        }
    }
}
