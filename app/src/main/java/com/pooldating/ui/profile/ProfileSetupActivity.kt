package com.pooldating.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.pooldating.R
import com.pooldating.ui.home.HomeActivity
import com.pooldating.utils.Result

class ProfileSetupActivity : AppCompatActivity() {

    private val viewModel: ProfileSetupViewModel by viewModels()

    private lateinit var etName: EditText
    private lateinit var etAge: EditText
    private lateinit var spinnerGender: Spinner
    private lateinit var etBio: EditText
    private lateinit var etInterests: EditText
    // private lateinit var spinnerCity: Spinner // REMOVED for Kochi-only launch
    private lateinit var btnSave: Button
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        android.util.Log.d("ProfileSetupActivity", "onCreate started")
        Toast.makeText(this, "PROFILE SETUP: Started", Toast.LENGTH_SHORT).show()
        
        try {
            setContentView(R.layout.activity_profile_setup)

            etName = findViewById(R.id.etName)
            etAge = findViewById(R.id.etAge)
            spinnerGender = findViewById(R.id.spinnerGender)
            etBio = findViewById(R.id.etBio)
            etInterests = findViewById(R.id.etInterests)
            // spinnerCity = findViewById(R.id.spinnerCity) // REMOVED
            btnSave = findViewById(R.id.btnSaveProfile)
            progressBar = findViewById(R.id.progressBar)

            setupSpinners()

            btnSave.setOnClickListener {
                val name = etName.text.toString()
                val age = etAge.text.toString()
                val gender = spinnerGender.selectedItem.toString()
                val bio = etBio.text.toString()
                val interests = etInterests.text.toString()
                val city = "Kochi" // HARDCODED for Kochi-only launch
                
                viewModel.saveProfile(name, age, gender, bio, interests, city)
            }

            observeViewModel()
            Toast.makeText(this, "PROFILE SETUP: Ready", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            android.util.Log.e("ProfileSetupActivity", "onCreate CRASHED", e)
            Toast.makeText(this, "PROFILE SETUP ERROR: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupSpinners() {
        val genderAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, viewModel.genders)
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerGender.adapter = genderAdapter

        // City selection hidden for launch
    }

    private fun observeViewModel() {
        viewModel.saveState.observe(this) { result ->
            when (result) {
                is Result.Loading -> {
                    progressBar.visibility = View.VISIBLE
                    btnSave.isEnabled = false
                }
                is Result.Success -> {
                    progressBar.visibility = View.GONE
                    btnSave.isEnabled = true
                    Toast.makeText(this, "Profile Created!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, HomeActivity::class.java))
                    finish()
                }
                is Result.Error -> {
                    progressBar.visibility = View.GONE
                    btnSave.isEnabled = true
                    Toast.makeText(this, "Error: ${result.exception.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
