package com.merino.ddfilms.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.merino.ddfilms.R
import com.merino.ddfilms.model.Credits

class CrewAdapter(
    var crewList: List<Credits.Crew>?
) : RecyclerView.Adapter<CrewAdapter.CrewViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CrewViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_person, parent, false)
        return CrewViewHolder(view)
    }

    override fun onBindViewHolder(holder: CrewViewHolder, position: Int) {
        val crewListSafe = crewList ?: return
        val crew = crewListSafe[position]

        holder.personName.text = crew.name
        holder.personRole.text = crew.job

        if (crew.profilePath != null) {
            Glide.with(holder.itemView.context)
                .load("https://image.tmdb.org/t/p/w500/" + crew.profilePath)
                .placeholder(R.drawable.ic_profile)
                .error(R.drawable.ic_close)
                .into(holder.personImage)
        } else {
            holder.personImage.setImageResource(R.drawable.ic_profile)
        }
    }

    override fun getItemCount(): Int = crewList?.size ?: 0

    class CrewViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val personImage: ImageView = itemView.findViewById(R.id.person_image)
        val personName: TextView = itemView.findViewById(R.id.person_name)
        val personRole: TextView = itemView.findViewById(R.id.person_role)
    }
}
