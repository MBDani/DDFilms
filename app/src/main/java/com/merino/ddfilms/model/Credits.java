package com.merino.ddfilms.model;

import android.os.Parcel;
import android.os.Parcelable;

import lombok.Data;

import java.util.List;
import java.util.Optional;

@Data
public class Credits implements Parcelable {
    private int id = 0;
    private List<Cast> cast;
    private List<Crew> crew;

    protected Credits(Parcel in) {
        id = in.readInt();
        cast = in.createTypedArrayList(Cast.CREATOR);
        crew = in.createTypedArrayList(Crew.CREATOR);
    }

    public static final Creator<Credits> CREATOR = new Creator<Credits>() {
        @Override
        public Credits createFromParcel(Parcel in) {
            return new Credits(in);
        }

        @Override
        public Credits[] newArray(int size) {
            return new Credits[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeTypedList(cast);
        dest.writeTypedList(crew);
    }

    @Data
    public static class Cast implements Parcelable {
        private boolean adult = true;
        private int gender = 0;
        private int id = 0;
        private String knownForDepartment;
        private String name;
        private String originalName;
        private double popularity = 0;
        private String profilePath;
        private int castId = 0;
        private String character;
        private String creditId;
        private int order = 0;

        protected Cast(Parcel in) {
            adult = in.readByte() != 0;
            gender = in.readInt();
            id = in.readInt();
            knownForDepartment = in.readString();
            name = in.readString();
            originalName = in.readString();
            popularity = in.readDouble();
            profilePath = in.readString();
            castId = in.readInt();
            character = in.readString();
            creditId = in.readString();
            order = in.readInt();
        }

        public static final Creator<Cast> CREATOR = new Creator<Cast>() {
            @Override
            public Cast createFromParcel(Parcel in) {
                return new Cast(in);
            }

            @Override
            public Cast[] newArray(int size) {
                return new Cast[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeByte((byte) (adult ? 1 : 0));
            dest.writeInt(gender);
            dest.writeInt(id);
            dest.writeString(knownForDepartment);
            dest.writeString(name);
            dest.writeString(originalName);
            dest.writeDouble(popularity);
            dest.writeString(profilePath);
            dest.writeInt(castId);
            dest.writeString(character);
            dest.writeString(creditId);
            dest.writeInt(order);
        }
    }

    @Data
    public static class Crew implements Parcelable {
        private boolean adult = true;
        private int gender = 0;
        private int id = 0;
        private String knownForDepartment;
        private String name;
        private String originalName;
        private double popularity = 0;
        private String profilePath;
        private String creditId;
        private String department;
        private String job;

        protected Crew(Parcel in) {
            adult = in.readByte() != 0;
            gender = in.readInt();
            id = in.readInt();
            knownForDepartment = in.readString();
            name = in.readString();
            originalName = in.readString();
            popularity = in.readDouble();
            profilePath = in.readString();
            creditId = in.readString();
            department = in.readString();
            job = in.readString();
        }

        public static String getDirector(List<Crew> crew) {
            Optional<String> directorName = crew.stream().filter(member -> "Director".equals(member.getJob())).map(Crew::getName).findFirst();
            return directorName.orElse("No director found");
        }

        public static final Creator<Crew> CREATOR = new Creator<Crew>() {
            @Override
            public Crew createFromParcel(Parcel in) {
                return new Crew(in);
            }

            @Override
            public Crew[] newArray(int size) {
                return new Crew[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeByte((byte) (adult ? 1 : 0));
            dest.writeInt(gender);
            dest.writeInt(id);
            dest.writeString(knownForDepartment);
            dest.writeString(name);
            dest.writeString(originalName);
            dest.writeDouble(popularity);
            dest.writeString(profilePath);
            dest.writeString(creditId);
            dest.writeString(department);
            dest.writeString(job);
        }
    }
}

