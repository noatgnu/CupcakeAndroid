package info.proteo.cupcake

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import info.proteo.cupcake.R
import info.proteo.cupcake.databinding.ActivitySupportInformationBinding
import info.proteo.cupcake.ui.instrument.SupportInformationFragment

@AndroidEntryPoint
class SupportInformationActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySupportInformationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySupportInformationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Support Information"

        val instrumentId = intent.getIntExtra("INSTRUMENT_ID", -1)
        if (instrumentId != -1) {
            if (savedInstanceState == null) {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, SupportInformationFragment.newInstance(instrumentId))
                    .commit()
            }
        } else {
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}