package com.pooldating.ui.profile

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.pooldating.R
import com.pooldating.utils.Result

class EditProfileActivity : AppCompatActivity() {

    private val viewModel: EditProfileViewModel by viewModels()

    private lateinit var tvName: TextView
    private lateinit var tvAge: TextView
    private lateinit var tvGender: TextView
    private lateinit var etBio: EditText
    private lateinit var etInterests: EditText
    private lateinit var spinnerCity: Spinner
    private lateinit var btnUpdate: Button
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        tvName = findViewById(R.id.tvNameReadonly)
        tvAge = findViewById(R.id.tvAgeReadonly)
        tvGender = findViewById(R.id.tvGenderReadonly)
        etBio = findViewById(R.id.etBio)
        etInterests = findViewById(R.id.etInterests)
        spinnerCity = findViewById(R.id.spinnerCity)
        btnUpdate = findViewById(R.id.btnUpdateProfile)
        progressBar = findViewById(R.id.progressBar)

        setupSpinners()

        btnUpdate.setOnClickListener {
            val bio = etBio.text.toString()
            val interests = etInterests.text.toString()
            val city = spinnerCity.selectedItem.toString()
            
            viewModel.updateProfile(bio, interests, city)
        }
        
        findViewById<Button>(R.id.btnAdminMatchmaking).setOnClickListener {
            viewModel.triggerAdminMatchmaking()
            Toast.makeText(this, "Triggered Matchmaking...", Toast.LENGTH_SHORT).show()
        }

        observeViewModel()
        viewModel.loadUserData()
    }

    private fun setupSpinners() {
        val cityAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, viewModel.cities)
        cityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCity.adapter = cityAdapter
    }

    private fun observeViewModel() {
        viewModel.userData.observe(this) { result ->
            when (result) {
                is Result.Loading -> {
                    progressBar.visibility = View.VISIBLE
                }
                is Result.Success -> {
                    progressBar.visibility = View.GONE
                    val user = result.data
                    if (user != null) {
                        tvName.text = "Name: ${user.name ?: "Unknown"}"
                        tvAge.text = "Age: ${user.age ?: "N/A"}"
                        tvGender.text = "Gender: ${user.gender ?: "Not Specified"}"
                        etBio.setText(user.bio ?: "")
                        etInterests.setText(user.interests ?: "")
                        
                        // Set spinner selection
                        val cityIndex = viewModel.cities.indexOf(user.city)
                        if (cityIndex >= 0) {
                            spinnerCity.setSelection(cityIndex)
                        }
                    }
                }
                is Result.Error -> {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show()
                }
            }
        }
        
        // Disable city change when user is in an active pool
        viewModel.hasActiveMembership.observe(this) { hasActive ->
            spinnerCity.isEnabled = !hasActive
            if (hasActive) {
                spinnerCity.alpha = 0.5f
                // Show a hint that city cannot be changed
                Toast.makeText(this, "Cannot change city while in a pool", Toast.LENGTH_SHORT).show()
            } else {
                spinnerCity.alpha = 1.0f
            }
        }

        viewModel.updateState.observe(this) { result ->
            when (result) {
                is Result.Loading -> {
                    progressBar.visibility = View.VISIBLE
                    btnUpdate.isEnabled = false
                }
                is Result.Success -> {
                    progressBar.visibility = View.GONE
                    btnUpdate.isEnabled = true
                    Toast.makeText(this, "Profile Updated!", Toast.LENGTH_SHORT).show()
                    finish() // Go back to Home
                }
                is Result.Error -> {
                    progressBar.visibility = View.GONE
                    btnUpdate.isEnabled = true
                    Toast.makeText(this, "Error: ${result.exception.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
