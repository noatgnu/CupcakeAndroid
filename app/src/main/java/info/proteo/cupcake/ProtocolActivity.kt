package info.proteo.cupcake

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import info.proteo.cupcake.databinding.ActivityProtocolBinding

@AndroidEntryPoint
class ProtocolActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProtocolBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProtocolBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}