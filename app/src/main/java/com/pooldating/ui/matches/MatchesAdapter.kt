package com.pooldating.ui.matches

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.pooldating.R
import com.pooldating.data.model.MatchWithUser

class MatchesAdapter : RecyclerView.Adapter<MatchesAdapter.MatchViewHolder>() {

    private val matches = mutableListOf<MatchWithUser>()

    fun setMatches(newMatches: List<MatchWithUser>) {
        matches.clear()
        matches.addAll(newMatches)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MatchViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_match, parent, false)
        return MatchViewHolder(view)
    }

    override fun onBindViewHolder(holder: MatchViewHolder, position: Int) {
        val matchWithUser = matches[position]
        holder.bind(matchWithUser)
    }

    override fun getItemCount(): Int = matches.size

    class MatchViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvInitials: TextView = itemView.findViewById(R.id.tvInitials)
        private val tvName: TextView = itemView.findViewById(R.id.tvMatchName)
        private val tvLocation: TextView = itemView.findViewById(R.id.tvMatchLocation)
        private val tvCompatibility: TextView = itemView.findViewById(R.id.tvCompatibility)
        private val tvInterests: TextView = itemView.findViewById(R.id.tvMatchInterests)
        private val btnStartChat: View = itemView.findViewById(R.id.btnStartChat)

        fun bind(item: MatchWithUser) {
            val user = item.otherUser
            val match = item.match
            
            // Name & Age
            val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            val isUserA = currentUserId == match.user_a
            val nameFromMatch = if (isUserA) match.user_b_name else match.user_a_name
            val finalName = if (nameFromMatch.isNotEmpty() && nameFromMatch != "Unknown") nameFromMatch else (user?.name ?: "Unknown")
            
            tvName.text = "$finalName, ${user?.age ?: "?"}"
            
            // Initials
            tvInitials.text = finalName.take(1).uppercase()
            
            // Location
            tvLocation.text = user?.city ?: "Kochi"
            
            // Compatibility
            tvCompatibility.text = "${match.compatibility_score}% Match"
            
            // Interests
            val interestsStr = user?.interests
            if (!interestsStr.isNullOrEmpty()) {
                // If it's already comma separated or just a string, show it.
                // Replace any ugly brackets if they exist (legacy)
                tvInterests.text = interestsStr.replace("[", "").replace("]", "").replace("\"", "")
            } else {
                tvInterests.text = "No interests selected"
            }
            
            // Start Chat
            btnStartChat.setOnClickListener {
                // TODO: Launch Chat Activity
                // context.startActivity(...)
                android.widget.Toast.makeText(itemView.context, "Starting conversation with $finalName...", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
}
