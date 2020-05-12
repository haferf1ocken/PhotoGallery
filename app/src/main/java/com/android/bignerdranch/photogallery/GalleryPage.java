package com.android.bignerdranch.photogallery;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class GalleryPage {

    PhotoResults photos;
    String stat;
    public List<GalleryItem> getGalleryItemList() {
        return photos.getPhotoList();
    }
}

class PhotoResults {

    int page;
    int pages;
    int perpage;
    int total;

    @SerializedName("photo")
    List<GalleryItem> photoList;
    List<GalleryItem> getPhotoList() {
        return photoList;
    }
}
