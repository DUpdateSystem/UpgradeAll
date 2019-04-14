package net.xzos.upgraderall;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class UpgradeItemSettingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upgrade_item_setting);
        Button addButton = findViewById(R.id.addButton);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText editName = findViewById(R.id.editName);
                EditText editUrl = findViewById(R.id.editUrl);
                String name = editName.getText().toString();
                String api_url = editUrl.getText().toString();
                Intent intentService = new Intent(UpgradeItemSettingActivity.this, DownloadService.class);
                String api = "github";
                intentService.putExtra("api", api);
                intentService.putExtra("api_url", api_url);
                startService(intentService);
            }
        });
    }
}
