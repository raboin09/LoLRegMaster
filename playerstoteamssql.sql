insert ignore into `PlayerToTeam`(select playerId, teamId from `Teams` left join `Players` on Players.playerId = Teams.adcPlayerId);
insert ignore into `PlayerToTeam`(select playerId, teamId from `Teams` left join `Players` on Players.playerId = Teams.midPlayerId);
insert ignore into `PlayerToTeam`(select playerId, teamId from `Teams` left join `Players` on Players.playerId = Teams.jngPlayerId);
insert ignore into `PlayerToTeam`(select playerId, teamId from `Teams` left join `Players` on Players.playerId = Teams.supPlayerId);
insert ignore into `PlayerToTeam`(select playerId, teamId from `Teams` left join `Players` on Players.playerId = Teams.topPlayerId);