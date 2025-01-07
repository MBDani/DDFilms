package com.merino.ddfilms.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.merino.ddfilms.R;
import com.merino.ddfilms.model.Credits;

import java.util.List;

import lombok.Setter;

@Setter
public class CastAdapter extends RecyclerView.Adapter<CastAdapter.CastViewHolder> {

    private List<Credits.Cast> castList;

    public CastAdapter(List<Credits.Cast> castList) {
        this.castList = castList;
    }

    @NonNull
    @Override
    public CastViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_person, parent, false);
        return new CastViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CastViewHolder holder, int position) {
        Credits.Cast cast = castList.get(position);

        // Configura el nombre y el personaje
        holder.personName.setText(cast.getName());
        holder.personRole.setText(cast.getCharacter());

        // Comprobar si el profilePath no es null antes de cargar la imagen
        if (cast.getProfilePath() != null) {
            Glide.with(holder.itemView.getContext())
                    .load("https://image.tmdb.org/t/p/w500/" + cast.getProfilePath())
                    .placeholder(R.drawable.ic_profile) // Imagen por defecto mientras carga
                    .error(R.drawable.ic_close) // Imagen si ocurre un error al cargar
                    .into(holder.personImage);
        } else {
            // Si no hay profilePath, usa una imagen por defecto
            holder.personImage.setImageResource(R.drawable.ic_profile);
        }
    }

    @Override
    public int getItemCount() {
        return castList != null ? castList.size() : 0;
    }

    static class CastViewHolder extends RecyclerView.ViewHolder {
        ImageView personImage;
        TextView personName;
        TextView personRole;

        public CastViewHolder(@NonNull View itemView) {
            super(itemView);
            personImage = itemView.findViewById(R.id.person_image);
            personName = itemView.findViewById(R.id.person_name);
            personRole = itemView.findViewById(R.id.person_role);
        }
    }
}
