package info.proteo.cupcake

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import info.proteo.cupcake.databinding.ActivityInstrumentBinding
import info.proteo.cupcake.ui.instrument.InstrumentFragment

@AndroidEntryPoint
class InstrumentActivity : AppCompatActivity() {
    private lateinit var binding: ActivityInstrumentBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInstrumentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Instruments"
        binding.toolbar.navigationIcon?.setTint(
            ContextCompat.getColor(this, R.color.white)
        )

        initializeInstruments()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun initializeInstruments() {
        val instrumentFragment = InstrumentFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.instrument_container, instrumentFragment)
            .commit()
    }
}