package com.example.android.spotifystreamer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import kaaes.spotify.webapi.android.models.Image;

/**
 * Created by Alexander on 6/23/2015.
 */
class ImageHelper {
    List<SortableImage> sortedImages = new ArrayList<SortableImage>();
    public ImageHelper(List<Image> images) {
        for (Image img : images) {
            sortedImages.add(new SortableImage(img));
        }
        Collections.sort(sortedImages);
    }
    // Pick up smallest image which is larger then image view by any dimension
    // so that when displaying in the view it will be down-scaled a little bit and have a good
    // quality
    public Image getSuitableImageForImageView(int viewWidth, int viewHeight) {
        Image img = null;
        // Images sorted in ascending order, i.e. from small to larger images
        for (SortableImage srtImg : sortedImages) {
            img = srtImg.getImage();
            if (img.width >= viewWidth || img.height >= viewHeight)
                break; //will use this image
        }
        return img;
    }
    class SortableImage implements Comparable {
        private Image image;
        public SortableImage(Image image) {
            this.image = image;
        }
        public Image getImage() {
            return image;
        }
        public int compareTo(Object anotherImage) throws ClassCastException {
            if (!(anotherImage instanceof SortableImage))
                throw new ClassCastException("A SortableImage object expected.");
            Image anotherImg = ((SortableImage) anotherImage).getImage();
            // Images are compared by sum of their dimensions
            int anotherDimSum = anotherImg.height + anotherImg.width;
            int thisDimSum = this.image.height + this.image.width;
            return thisDimSum - anotherDimSum;
        }
    }
}