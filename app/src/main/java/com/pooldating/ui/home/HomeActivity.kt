package com.pooldating.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.GridLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.pooldating.R
import com.pooldating.data.model.Circle
import com.pooldating.ui.login.LoginActivity
import com.pooldating.ui.profile.EditProfileActivity
import com.pooldating.ui.matches.MatchesActivity
import com.pooldating.ui.chat.ChatsListActivity
import com.pooldating.utils.Result
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.activity.result.contract.ActivityResultContracts

class HomeActivity : AppCompatActivity() {

    private val viewModel: HomeViewModel by viewModels()
    
    // Notification Permission Launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(this, "Notifications needed for matches/chats", Toast.LENGTH_SHORT).show()
        }
    }
    
    private lateinit var googleSignInClient: com.google.android.gms.auth.api.signin.GoogleSignInClient

    // Header UI
    private lateinit var tvUserName: TextView
    private lateinit var tvCredits: TextView
    
    // Circle UI
    private lateinit var tvCircleName: TextView
    private lateinit var tvCircleCity: TextView
    private lateinit var tvCircleStatus: TextView
    private lateinit var tvCircleDescription: TextView

    private lateinit var tvInCircle: TextView
    private lateinit var tvSpotsOpen: TextView
    private lateinit var tvDaysLeft: TextView
    
    // Actions
    private lateinit var btnJoinCircle: MaterialButton
    private lateinit var btnViewCircle: MaterialButton
    
    // Journey
    private lateinit var tvNoHistory: TextView
    
    // Bottom Nav
    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        
        // Initialize Google Sign In Client for logout
        val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(
            com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN
        ).requestIdToken(getString(R.string.default_web_client_id)).build()
        googleSignInClient = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(this, gso)

        initViews()
        setupBottomNav()
        setupClickListeners()
        observeViewModel()
        
        // Register FCM token for push notifications
        registerFcmToken()
        askNotificationPermission()
    }

    private fun initViews() {
        // Header
        tvUserName = findViewById(R.id.tvUserName)
        tvCredits = findViewById(R.id.tvCredits)
        
        // Circle
        tvCircleName = findViewById(R.id.tvCircleName)
        tvCircleCity = findViewById(R.id.tvCircleCity)
        tvCircleStatus = findViewById(R.id.tvCircleStatus)
        tvCircleDescription = findViewById(R.id.tvCircleDescription)

        tvInCircle = findViewById(R.id.tvInCircle)
        tvSpotsOpen = findViewById(R.id.tvSpotsOpen)
        tvDaysLeft = findViewById(R.id.tvDaysLeft)
        
        // Actions
        btnJoinCircle = findViewById(R.id.btnJoinCircle)
        btnViewCircle = findViewById(R.id.btnViewCircle)
        
        // Journey
        tvNoHistory = findViewById(R.id.tvNoHistory)
        
        // Bottom Nav
        bottomNav = findViewById(R.id.bottomNav)
        bottomNav.selectedItemId = R.id.nav_home
    }

    private fun setupBottomNav() {
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true // Already here
                R.id.nav_connections -> {
                    val pool = viewModel.membershipPool.value ?: viewModel.activePool.value
                    if (pool != null && (pool.status == "completed" || pool.status == "matching")) {
                        startActivity(Intent(this, MatchesActivity::class.java).apply {
                            putExtra("POOL_ID", pool.pool_id)
                        })
                    } else {
                        Toast.makeText(this, "Join a Circle first!", Toast.LENGTH_SHORT).show()
                    }
                    true
                }
                R.id.nav_chats -> {
                    startActivity(Intent(this, ChatsListActivity::class.java))
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, EditProfileActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    private fun setupClickListeners() {
        // Tap credits to add more (dev helper)
        // Buy credits button
        findViewById<View>(R.id.btnBuyCredits).setOnClickListener {
            viewModel.purchaseCredits(5)
            Toast.makeText(this, "Redirecting to payments...", Toast.LENGTH_SHORT).show()
        }
        
        btnJoinCircle.setOnClickListener {
            btnJoinCircle.isEnabled = false
            btnJoinCircle.text = "Joining..."
            viewModel.joinPool()
        }
        
        btnViewCircle.setOnClickListener {
            val pool = viewModel.membershipPool.value ?: viewModel.activePool.value
            if (pool != null) {
                startActivity(Intent(this, MatchesActivity::class.java).apply {
                    putExtra("POOL_ID", pool.pool_id)
                })
            }
        }
    }

    private fun observeViewModel() {
        // User Data
        viewModel.currentUser.observe(this) { user ->
            if (user != null) {
                tvUserName.text = user.name ?: "User"
                
                // Hide credits for females, join is free for them
                if (user.gender == "female") {
                    findViewById<View>(R.id.layoutCreditsDisplay).visibility = View.GONE
                    findViewById<View>(R.id.btnBuyCredits).visibility = View.GONE
                    btnJoinCircle.text = "Enter the Circle"
                }
            }
        }
        
        // Credits
        viewModel.creditBalance.observe(this) { balance ->
            tvCredits.text = balance.toString()
            updateJoinButtonText(balance)
        }

        // Active Pool (for joining)
        viewModel.activePool.observe(this) { pool ->
            updateCircleUI(pool, viewModel.myMembership.value)
        }
        
        // Membership
        viewModel.myMembership.observe(this) { membership ->
            updateCircleUI(viewModel.activePool.value, membership)
        }
        
        // Previous pool for viewing matches
        viewModel.membershipPool.observe(this) { pool ->
            if (pool != null && pool.status == "completed") {
                tvNoHistory.visibility = View.GONE
                // Could show past circles here
            }
        }

        // Join State
        viewModel.joinState.observe(this) { result ->
            when (result) {
                is Result.Loading -> {
                    btnJoinCircle.isEnabled = false
                    btnJoinCircle.text = "Joining..."
                }
                is Result.Success -> {
                    btnJoinCircle.isEnabled = false
                    btnJoinCircle.text = "You're in! ✓"
                    Toast.makeText(this, "Welcome to the Circle!", Toast.LENGTH_SHORT).show()
                }
                is Result.Error -> {
                    btnJoinCircle.isEnabled = true
                    updateJoinButtonText(viewModel.creditBalance.value ?: 0)
                    Toast.makeText(this, "Failed: ${result.exception.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        // Sign Out
        viewModel.signOutState.observe(this) { signedOut ->
            if (signedOut) {
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        }
        
        // Purchase
        viewModel.purchaseState.observe(this) { result ->
            when (result) {
                is Result.Success -> {
                    Toast.makeText(this, "+${result.data.creditsAdded} credits!", Toast.LENGTH_SHORT).show()
                }
                is Result.Error -> {
                    Toast.makeText(this, "Purchase failed", Toast.LENGTH_SHORT).show()
                }
                else -> {}
            }
        }
    }

    private fun updateCircleUI(pool: Circle?, membership: com.pooldating.data.model.CircleMembership?) {
        if (pool == null) {
            tvCircleName.text = "No Active Circle"
            tvCircleStatus.text = "WAITING"
            tvCircleDescription.text = "Check back soon for a new Circle in your city."
            btnJoinCircle.visibility = View.GONE
            return
        }
        
        // Update Circle header
        val month = java.text.SimpleDateFormat("MMMM", java.util.Locale.getDefault()).format(
            pool.created_at?.toDate() ?: java.util.Date()
        )
        tvCircleName.text = "$month Circle"
        tvCircleCity.text = pool.city ?: "Your City"
        
        // Status badge
        val statusText = when (pool.status) {
            "joining" -> "FORMING"
            "validating", "matching" -> "CONNECTING"
            "completed" -> "REVEALED"
            else -> pool.status?.uppercase() ?: "UNKNOWN"
        }
        tvCircleStatus.text = statusText
        
        // Description
        val description = when (pool.status) {
            "joining" -> {
                val deadline = pool.join_deadline?.toDate()
                if (deadline != null) {
                    val daysLeft = ((deadline.time - System.currentTimeMillis()) / (24 * 60 * 60 * 1000)).coerceAtLeast(0)
                    "Circle closes in ${daysLeft}d"
                } else {
                    "Circle is forming"
                }
            }
            "matching" -> "Connections being made..."
            "completed" -> "View your connections!"
            else -> "Circle status: ${pool.status}"
        }
        tvCircleDescription.text = description
        
        // Stats
        val totalCount = (pool.male_count ?: 0) + (pool.female_count ?: 0) + 
                        (pool.buffer_m_count ?: 0) + (pool.buffer_f_count ?: 0)
        val maxSize = 50
        tvInCircle.text = totalCount.toString()
        tvSpotsOpen.text = (maxSize - totalCount).coerceAtLeast(0).toString()
        
        val deadline = pool.join_deadline?.toDate()
        if (deadline != null) {
            val daysLeft = ((deadline.time - System.currentTimeMillis()) / (24 * 60 * 60 * 1000)).coerceAtLeast(0)
            tvDaysLeft.text = "${daysLeft}d"
        }
        
        // Update dots grid
        updateDotsGrid(pool)
        
        // Button state
        val isMember = membership != null && membership.pool_id == pool.pool_id
        val isActiveOrBuffer = membership?.status in listOf("active", "buffer")
        
        when {
            isMember && isActiveOrBuffer -> {
                btnJoinCircle.visibility = View.GONE
                btnViewCircle.visibility = if (pool.status == "completed" || pool.status == "matching") View.VISIBLE else View.GONE
                if (btnViewCircle.visibility == View.GONE) {
                    btnJoinCircle.visibility = View.VISIBLE
                    btnJoinCircle.isEnabled = false
                    btnJoinCircle.text = if (membership?.status == "buffer") "Awaiting..." else "You're in ✓"
                }
            }
            pool.status == "joining" -> {
                btnJoinCircle.visibility = View.VISIBLE
                btnViewCircle.visibility = View.GONE
                btnJoinCircle.isEnabled = true
                updateJoinButtonText(viewModel.creditBalance.value ?: 0)
            }
            pool.status == "completed" -> {
                btnJoinCircle.visibility = View.GONE
                btnViewCircle.visibility = View.VISIBLE
            }
            else -> {
                btnJoinCircle.visibility = View.GONE
                btnViewCircle.visibility = View.GONE
            }
        }
    }
    
    private fun updateJoinButtonText(credits: Int) {
        val user = viewModel.currentUser.value
        if (user?.gender == "female") {
            btnJoinCircle.text = "Enter the Circle"
        } else {
            btnJoinCircle.text = "Enter the Circle • 10 credits"
            if (credits < 10) {
                btnJoinCircle.isEnabled = false
                btnJoinCircle.text = "Need more credits"
            }
        }
    }
    
    private fun updateDotsGrid(pool: Circle) {
        val gridDots = findViewById<GridLayout>(R.id.gridDots)
        gridDots.removeAllViews()
        
        val totalSlots = 50 // Combined grid
        
        val maleCount = (pool.male_count ?: 0) + (pool.buffer_m_count ?: 0)
        val femaleCount = (pool.female_count ?: 0) + (pool.buffer_f_count ?: 0)
        
        val maleColor = ContextCompat.getColor(this, R.color.cohort_male)
        val femaleColor = ContextCompat.getColor(this, R.color.cohort_female)
        val emptyColor = ContextCompat.getColor(this, R.color.border)
        
        // Update counts in legend
        findViewById<TextView>(R.id.tvMaleCount).text = "Male: $maleCount/25"
        findViewById<TextView>(R.id.tvFemaleCount).text = "Female: $femaleCount/25"
        
        // Dynamic sizing
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        
        // Padding calculation:
        // Root LinearLayout paddingHorizontal = 20dp
        // Dots container padding = 16dp
        // Total horizontal padding = 20 + 20 + 16 + 16 = 72dp
        val density = displayMetrics.density
        val totalPaddingPx = (72 * density).toInt()
        val availableWidth = screenWidth - totalPaddingPx
        val columnCount = 10
        
        val cellSize = availableWidth / columnCount
        val dotSize = (cellSize * 0.55).toInt() // Reduced size factor to 0.55
        val margin = (cellSize * 0.225).toInt() // Increased margin
        
        // Create 50 dots
        // 0-24: Male slots (Filled or Empty)
        // 25-49: Female slots (Filled or Empty)
        
        repeat(totalSlots) { i ->
            val dot = View(this)
            
            val params = GridLayout.LayoutParams()
            params.width = dotSize
            params.height = dotSize
            params.setMargins(margin, margin, margin, margin)
            dot.layoutParams = params
            
            val color = when {
                i < 25 -> { // Male Section
                    if (i < maleCount) maleColor else emptyColor
                }
                else -> { // Female Section (i >= 25)
                    val femaleIndex = i - 25
                    if (femaleIndex < femaleCount) femaleColor else emptyColor
                }
            }
            
            val drawable = android.graphics.drawable.GradientDrawable()
            drawable.shape = android.graphics.drawable.GradientDrawable.OVAL
            drawable.setColor(color)
            dot.background = drawable
            
            gridDots.addView(dot)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadUserData()
        bottomNav.selectedItemId = R.id.nav_home
    }
    
    private fun registerFcmToken() {
        com.google.firebase.messaging.FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                com.google.firebase.functions.FirebaseFunctions.getInstance()
                    .getHttpsCallable("registerDeviceToken")
                    .call(hashMapOf("fcm_token" to token))
            }
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
