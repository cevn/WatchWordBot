package com.raether.watchwordbot.user;

import javax.persistence.Entity;
import javax.persistence.Id;

import com.raether.watchwordbot.ranking.RatingValue;

@Entity
public class UserEntity {
	@Id
	private String userId;
	// @Transient
	private String username;
	private RatingValue rating;

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public RatingValue getRating() {
		return rating;
	}

	public void setRating(RatingValue rating) {
		this.rating = rating;
	}

}