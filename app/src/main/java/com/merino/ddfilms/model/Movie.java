package com.merino.ddfilms.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

import javax.annotation.Nullable;

import lombok.Data;

@Data
public class Movie implements Parcelable {
    private boolean adult;
    @SerializedName("backdrop_path")
    private String backdropPath;
    @SerializedName("genre_ids")
    private ArrayList genreIds;
    private int id;
    @SerializedName("original_language")
    private String originalLanguage;
    @SerializedName("original_title")
    private String originalTitle;
    private String overview;
    private double popularity;
    @SerializedName("poster_path")
    private String posterPath;
    @SerializedName("release_date")
    private String releaseDate;
    private String title;
    private boolean video;
    @SerializedName("vote_average")
    private double voteAverage;
    @SerializedName("vote_count")
    private int voteCount;
    @Nullable
    private String createdAt; // Campo para almacenar la fecha de creación en una lista
    @Nullable
    private String addedBy;

    // Constructor que lee desde un Parcel
    protected Movie(Parcel in) {
        adult = in.readByte() != 0;
        backdropPath = in.readString();
        genreIds = in.readArrayList(Integer.class.getClassLoader());
        id = in.readInt();
        originalLanguage = in.readString();
        originalTitle = in.readString();
        overview = in.readString();
        popularity = in.readDouble();
        posterPath = in.readString();
        releaseDate = in.readString();
        title = in.readString();
        video = in.readByte() != 0;
        voteAverage = in.readDouble();
        voteCount = in.readInt();
        createdAt = in.readString();
        addedBy = in.readString();
    }

    protected Movie() {
    }

    // Necesario para escribir el objeto en un Parcel
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte((byte) (adult ? 1 : 0));
        dest.writeString(backdropPath);
        dest.writeList(genreIds);
        dest.writeInt(id);
        dest.writeString(originalLanguage);
        dest.writeString(originalTitle);
        dest.writeString(overview);
        dest.writeDouble(popularity);
        dest.writeString(posterPath);
        dest.writeString(releaseDate);
        dest.writeString(title);
        dest.writeByte((byte) (video ? 1 : 0));
        dest.writeDouble(voteAverage);
        dest.writeInt(voteCount);
        dest.writeString(createdAt);
        dest.writeString(addedBy);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    // El CREATOR permite crear instancias del objeto desde un Parcel
    public static final Creator<Movie> CREATOR = new Creator<Movie>() {
        @Override
        public Movie createFromParcel(Parcel in) {
            return new Movie(in);
        }

        @Override
        public Movie[] newArray(int size) {
            return new Movie[size];
        }
    };

    public static Movie mapToMovie(Map<String, Object> movieMap) {
        Movie movie = new Movie();
        movie.setAdult((Boolean) movieMap.get("adult"));
        movie.setBackdropPath((String) movieMap.get("backdropPath"));
        movie.setGenreIds((ArrayList) movieMap.get("genreIds"));
        movie.setId(((Long) movieMap.get("id")).intValue());
        movie.setOriginalLanguage((String) movieMap.get("originalLanguage"));
        movie.setOriginalTitle((String) movieMap.get("originalTitle"));
        movie.setOverview((String) movieMap.get("overview"));
        movie.setPopularity((Double) movieMap.get("popularity"));
        movie.setPosterPath((String) movieMap.get("posterPath"));
        movie.setReleaseDate((String) movieMap.get("releaseDate"));
        movie.setTitle((String) movieMap.get("title"));
        movie.setVideo((Boolean) movieMap.get("video"));
        movie.setVoteAverage((Double) movieMap.get("voteAverage"));
        movie.setVoteCount(((Long) movieMap.get("voteCount")).intValue());
        movie.setCreatedAt((String) movieMap.get("createdAt"));
        movie.setAddedBy((String) movieMap.get("addedBy"));
        return movie;
    }
}