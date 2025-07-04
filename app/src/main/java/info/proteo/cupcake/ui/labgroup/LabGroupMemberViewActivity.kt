package info.proteo.cupcake.ui.labgroup

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * Activity for viewing lab group members (read-only)
 * Available to all users
 */
class LabGroupMemberViewActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val labGroupId = intent.getIntExtra("LAB_GROUP_ID", -1)
        val labGroupName = intent.getStringExtra("LAB_GROUP_NAME") ?: "Unknown"
        
        // TODO: Implement member view UI
        Toast.makeText(this, "Viewing members of \"$labGroupName\" - Coming Soon", Toast.LENGTH_LONG).show()
        
        finish()
    }
}