package com.merino.ddfilms.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

import java.util.List;

import lombok.Data;

@Data
public class MovieDetails implements Parcelable {
    private int id;
    private String title;
    private String overview;
    @SerializedName("release_date")
    private String releaseDate;
    private int runtime;
    private String tagline;
    private String status;
    @SerializedName("original_language")
    private String originalLanguage;
    @SerializedName("backdrop_path")
    private String backdropPath;
    @SerializedName("poster_path")
    private String posterPath;
    @SerializedName("vote_average")
    private double voteAverage;
    @SerializedName("vote_count")
    private int voteCount;
    private List<Genre> genres;

    protected MovieDetails(Parcel in) {
        id = in.readInt();
        title = in.readString();
        overview = in.readString();
        releaseDate = in.readString();
        runtime = in.readInt();
        tagline = in.readString();
        status = in.readString();
        originalLanguage = in.readString();
        backdropPath = in.readString();
        posterPath = in.readString();
        voteAverage = in.readDouble();
        voteCount = in.readInt();
        genres = in.createTypedArrayList(Genre.CREATOR);
    }



    public static final Creator<MovieDetails> CREATOR = new Creator<MovieDetails>() {
        @Override
        public MovieDetails createFromParcel(Parcel in) {
            return new MovieDetails(in);
        }

        @Override
        public MovieDetails[] newArray(int size) {
            return new MovieDetails[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(id);
        parcel.writeString(title);
        parcel.writeString(overview);
        parcel.writeString(releaseDate);
        parcel.writeInt(runtime);
        parcel.writeString(tagline);
        parcel.writeString(status);
        parcel.writeString(originalLanguage);
        parcel.writeString(backdropPath);
        parcel.writeString(posterPath);
        parcel.writeDouble(voteAverage);
        parcel.writeInt(voteCount);
        parcel.writeTypedList(genres);
    }

    public String getDuration() {
        int hours = runtime / 60;
        int minutes = runtime % 60;
        return hours > 0 ? hours + "h " + minutes + "m" : minutes + "m";
    }

    public static class Genre implements Parcelable {
        private int id;
        private String name;

        protected Genre(Parcel in) {
            id = in.readInt();
            name = in.readString();
        }

        public static final Creator<Genre> CREATOR = new Creator<Genre>() {
            @Override
            public Genre createFromParcel(Parcel in) {
                return new Genre(in);
            }

            @Override
            public Genre[] newArray(int size) {
                return new Genre[size];
            }
        };

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeInt(id);
            parcel.writeString(name);
        }

        @Override
        public int describeContents() {
            return 0;
        }
    }
}
