package info.proteo.cupcake

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import info.proteo.cupcake.ui.barcode.BarcodeScannerFragment

class BarcodeScannerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_barcode_scanner)

        if (savedInstanceState == null) {
            val fragment = BarcodeScannerFragment()

            // Set up result listener
            supportFragmentManager.setFragmentResultListener(
                "barcode_result",
                this
            ) { _, bundle ->
                val barcode = bundle.getString("barcode")
                barcode?.let {
                    val intent = Intent().apply {
                        putExtra("barcode_result", it)
                    }
                    setResult(Activity.RESULT_OK, intent)
                    finish()
                }
            }

            supportFragmentManager.beginTransaction()
                .replace(R.id.container, fragment)
                .commit()
        }
    }
}