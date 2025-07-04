package info.proteo.cupcake.ui.labgroup

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import info.proteo.cupcake.R

/**
 * Activity for managing lab group members (staff only)
 * Allows adding/removing users from lab groups
 */
class LabGroupMemberManagementActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val labGroupId = intent.getIntExtra("LAB_GROUP_ID", -1)
        val labGroupName = intent.getStringExtra("LAB_GROUP_NAME") ?: "Unknown"
        
        // TODO: Implement member management UI
        Toast.makeText(this, "Member management for \"$labGroupName\" - Coming Soon", Toast.LENGTH_LONG).show()
        
        finish()
    }
}