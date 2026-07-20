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

class CastAdapter(
    var castList: List<Credits.Cast>?
) : RecyclerView.Adapter<CastAdapter.CastViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CastViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_person, parent, false)
        return CastViewHolder(view)
    }

    override fun onBindViewHolder(holder: CastViewHolder, position: Int) {
        val castListSafe = castList ?: return
        val cast = castListSafe[position]

        holder.personName.text = cast.name
        holder.personRole.text = cast.character

        if (cast.profilePath != null) {
            Glide.with(holder.itemView.context)
                .load("https://image.tmdb.org/t/p/w500/" + cast.profilePath)
                .placeholder(R.drawable.ic_profile)
                .error(R.drawable.ic_close)
                .into(holder.personImage)
        } else {
            holder.personImage.setImageResource(R.drawable.ic_profile)
        }
    }

    override fun getItemCount(): Int = castList?.size ?: 0

    class CastViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val personImage: ImageView = itemView.findViewById(R.id.person_image)
        val personName: TextView = itemView.findViewById(R.id.person_name)
        val personRole: TextView = itemView.findViewById(R.id.person_role)
    }
}
