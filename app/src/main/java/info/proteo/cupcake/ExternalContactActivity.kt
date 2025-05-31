package info.proteo.cupcake

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import info.proteo.cupcake.ui.contact.ExternalContactFragment

@AndroidEntryPoint
class ExternalContactActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_external_contact)
        Log.d("ExternalContactActivity", "onCreate called")

        val supportInfoId = intent.getIntExtra("SUPPORT_INFO_ID", -1)
        Log.d("ExternalContactActivity", "Support Info ID: $supportInfoId")
        if (savedInstanceState == null && supportInfoId != -1) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, ExternalContactFragment.newInstance(supportInfoId))
                .commit()
        }
    }
}