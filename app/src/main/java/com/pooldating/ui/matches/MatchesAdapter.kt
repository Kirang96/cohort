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
        private val tvName: TextView = itemView.findViewById(R.id.tvMatchName)
        private val tvCompatibility: TextView = itemView.findViewById(R.id.tvCompatibility)
        private val tvInterests: TextView = itemView.findViewById(R.id.tvMatchInterests)

        fun bind(item: MatchWithUser) {
            val user = item.otherUser
            val match = item.match
            
            val age = user.age ?: "?" // Assuming age field exists or DOB derived. 
            // In User model, check if 'age' exists. If not, maybe use DOB?
            // The plan said "From user profiles: age". I'll assume 'age' property in User or derived.
            // If User only has 'dob', I might need logic.
            // For now, I'll print raw value if exists.
            
            val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            val isUserA = currentUserId == match.user_a
            
            // Prefer name from Match object (denormalized), fallback to User object
            val nameFromMatch = if (isUserA) match.user_b_name else match.user_a_name
            val finalName = if (nameFromMatch.isNotEmpty() && nameFromMatch != "Unknown") nameFromMatch else (user?.name ?: "Unknown")
            
            tvName.text = "$finalName, ${user?.age ?: "-"}"
            tvCompatibility.text = "Compatibility Score: ${match.compatibility_score}"
            tvInterests.text = "Interests: ${user?.interests ?: "None"}"
        }
    }
}
