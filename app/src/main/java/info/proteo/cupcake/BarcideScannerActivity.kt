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
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, BarcodeScannerFragment().apply {
                    setOnBarcodeDetectedListener { barcode ->
                        val intent = Intent().apply {
                            putExtra("barcode_result", barcode)
                        }
                        setResult(Activity.RESULT_OK, intent)
                        finish()
                    }
                })
                .commit()
        }
    }
}