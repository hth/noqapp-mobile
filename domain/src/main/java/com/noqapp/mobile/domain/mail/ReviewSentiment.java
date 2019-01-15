package com.noqapp.mobile.domain.mail;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import com.google.gson.annotations.SerializedName;

/**
 * hitender
 * 2019-01-15 13:09
 */
@SuppressWarnings ({
    "PMD.BeanMembersShouldSerialize",
    "PMD.LocalVariableCouldBeFinal",
    "PMD.MethodArgumentCouldBeFinal",
    "PMD.LongVariable"
})
@JsonPropertyOrder(alphabetic = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReviewSentiment {
    @SuppressWarnings ({"unused"})
    @SerializedName("storeName")
    private String storeName;

    @SuppressWarnings ({"unused"})
    @SerializedName ("reviewerName")
    private String reviewerName;

    @SuppressWarnings ({"unused"})
    @SerializedName ("reviewerPhone")
    private String reviewerPhone;

    @SerializedName ("ratingCount")
    private int ratingCount;

    @SerializedName ("hourSaved")
    private int hourSaved;

    @SerializedName ("review")
    private String review;

    @SerializedName ("sentiment")
    private String sentiment;

    @SerializedName ("sentimentWatcherEmail")
    private String sentimentWatcherEmail;

    private ReviewSentiment(
        String storeName,
        String reviewerName,
        String reviewerPhone,
        int ratingCount,
        int hourSaved,
        String review,
        String sentiment,
        String sentimentWatcherEmail
    ) {
        this.storeName = storeName;
        this.reviewerName = reviewerName;
        this.reviewerPhone = reviewerPhone;
        this.ratingCount = ratingCount;
        this.hourSaved = hourSaved;
        this.review = review;
        this.sentiment = sentiment;
        this.sentimentWatcherEmail = sentimentWatcherEmail;
    }

    public static ReviewSentiment newInstance(
        String storeName,
        String reviewerName,
        String reviewerPhone,
        int ratingCount,
        int hourSaved,
        String review,
        String sentiment,
        String sentimentWatcherEmail
    ) {
        return new ReviewSentiment(
            storeName,
            reviewerName,
            reviewerPhone,
            ratingCount,
            hourSaved,
            review,
            sentiment,
            sentimentWatcherEmail);
    }
}
