package info.proteo.cupcake

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

import android.content.Intent
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import info.proteo.cupcake.data.local.dao.user.UserPreferencesDao
import info.proteo.cupcake.ui.login.LoginFragment
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {

    @Inject
    lateinit var userPreferencesDao: UserPreferencesDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        lifecycleScope.launch {
            delay(1000)
            var hasLoggedInUser: Boolean = false
            val userPreference = userPreferencesDao.getCurrentlyActivePreference()
            if (userPreference != null) {
                hasLoggedInUser = true
            }

            val intent = if (hasLoggedInUser) {
                Intent(this@SplashActivity, MainActivity::class.java)
            } else {
                Intent(this@SplashActivity, LoginActivity::class.java)
            }

            startActivity(intent)
            finish()
        }
    }
}