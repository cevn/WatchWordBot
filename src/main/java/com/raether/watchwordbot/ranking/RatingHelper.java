package com.raether.watchwordbot.ranking;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import jskills.GameInfo;
import jskills.IPlayer;
import jskills.ITeam;
import jskills.Rating;
import jskills.Team;
import jskills.trueskill.FactorGraphTrueSkillCalculator;

import org.apache.commons.lang3.ArrayUtils;
import org.hibernate.Session;

import com.raether.watchwordbot.Faction;
import com.raether.watchwordbot.Player;
import com.raether.watchwordbot.TurnOrder;
import com.raether.watchwordbot.WatchWordLobby;
import com.raether.watchwordbot.user.UserEntity;
import com.raether.watchwordbot.user.UserHelper;
import com.ullink.slack.simpleslackapi.SlackUser;

public class RatingHelper {

	public static GameInfo getGameInfo() {
		return GameInfo.getDefaultGameInfo();
	}

	public static void addDefaultRatingToUser(UserEntity entity) {
		entity.setRating(new RatingValue(getGameInfo().getDefaultRating()));
	}

	public static void updatePlayerRatings(List<Faction> victors,
			List<Faction> losers, WatchWordLobby lobby, Session session) {
		System.out.println("Updating player ratings...");
		GameInfo gameInfo = getGameInfo();
		Collection<ITeam> teams = new ArrayList<ITeam>();
		List<Integer> rankings = new ArrayList<Integer>();
		final int WINNER = 1;// lower = better
		final int LOSER = 2;

		for (Faction victor : victors) {
			teams.add(buildITeam(victor, gameInfo, lobby, session));
			rankings.add(WINNER);
		}
		for (Faction loser : losers) {
			teams.add(buildITeam(loser, gameInfo, lobby, session));
			rankings.add(LOSER);
		}

		FactorGraphTrueSkillCalculator calculator = new FactorGraphTrueSkillCalculator();
		Map<IPlayer, Rating> newRankings = calculator.calculateNewRatings(
				gameInfo, teams,
				ArrayUtils.toPrimitive(rankings.toArray(new Integer[] {})));
		for (IPlayer player : newRankings.keySet()) {
			@SuppressWarnings("unchecked")
			jskills.Player<Player> castPlayer = (jskills.Player<Player>) player;
			SlackUser user = lobby.getUser(castPlayer.getId());

			UserEntity entity = UserHelper.readOrCreateUserEntity(user.getId(),
					user.getUserName(), session);
			updateRatingForPlayer(entity, newRankings.get(player), session);
		}
	}

	private static void updateRatingForPlayer(UserEntity entity,
			Rating newRating, Session session) {

		System.out.println("Setting rating for " + entity.getUserId()
				+ " from " + entity.getRating().getMean() + "("
				+ entity.getRating().getStandardDeviation() + ")" + " to "
				+ newRating.getMean() + "(" + newRating.getStandardDeviation()
				+ ")");
		entity.setRating(new RatingValue(newRating));
		session.saveOrUpdate(entity);
	}

	private static ITeam buildITeam(Faction faction, GameInfo info,
			WatchWordLobby lobby, Session session) {

		Team team = new Team();
		for (Player player : faction.getAllPlayers()) {
			SlackUser user = lobby.getUser(player);

			String slackId = user.getId();
			String slackName = user.getUserName();
			jskills.Player<Player> jskillsPlayer = new jskills.Player<Player>(
					player);
			UserEntity entity = UserHelper.readOrCreateUserEntity(slackId,
					slackName, session);
			team.addPlayer(jskillsPlayer, entity.getRating().createRating());
		}
		return team;
	}

	public static double getMatchQuality(TurnOrder turnOrder,
			WatchWordLobby lobby, Session session) {
		List<ITeam> teams = new ArrayList<ITeam>();
		GameInfo info = getGameInfo();
		for (Faction faction : turnOrder.getAllFactions()) {
			teams.add(buildITeam(faction, info, lobby, session));
		}
		FactorGraphTrueSkillCalculator calculator = new FactorGraphTrueSkillCalculator();
		return calculator.calculateMatchQuality(info, teams);

	}
}