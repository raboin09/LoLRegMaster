package lolregression;

import net.rithms.riot.api.ApiConfig;
import net.rithms.riot.api.RiotApi;
import net.rithms.riot.api.RiotApiException;
import net.rithms.riot.api.endpoints.match.dto.MatchList;
import net.rithms.riot.api.endpoints.match.dto.MatchReference;
import net.rithms.riot.api.endpoints.summoner.dto.Summoner;
import net.rithms.riot.api.endpoints.match.dto.Participant;
import net.rithms.riot.api.endpoints.match.dto.ParticipantStats;
import net.rithms.riot.api.endpoints.match.dto.ParticipantIdentity;
import net.rithms.riot.api.endpoints.match.dto.ParticipantTimeline;
import net.rithms.riot.api.endpoints.match.dto.TeamStats;
import net.rithms.riot.api.endpoints.match.dto.ParticipantStats;
import net.rithms.riot.api.endpoints.static_data.dto.ChampionList;
import net.rithms.riot.constant.Platform;
import net.rithms.riot.api.endpoints.match.dto.Match;

import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.Date;
import java.time.Instant;
import java.time.ZoneId;

import java.math.BigInteger;

import java.util.Scanner;

import java.util.List;

import java.sql.*;
import java.util.Properties;

import java.io.File;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;

public class Main {

    public static void main(String args[]) throws RiotApiException {

        getMatches();
    }

    public static Connection dbConnect() {
        System.out.println("Connecting to MySQL Server...");

        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.out.println("JDBC Driver Missing!");
            e.printStackTrace();
        }

        Connection connection = null;

        try {
            connection = DriverManager.getConnection("jdbc:mysql://" + "loldbmysql.crhbfyx7fdgr.us-east-2.rds.amazonaws.com" + ":" + "3306/mysqlloldb", "remoteu", "password");
        } catch (SQLException e) {
            System.out.println("Connection Failed!:\n" + e.getMessage());
        }

        if (connection != null) {
            System.out.println("Connection Successful!");
        } else {
            System.out.println("Failed to make connection!");
        }

        return connection;
    }

    public static void insertPlayers(Connection posConnection, String matchStr, BigInteger playerId, int kills, int deaths, int assists, String firstBloodKill, String firstBloodAssist, String firstBloodTowerKill, String firstBloodTowerAssist, String summonerId, String champPlayed) {

        try {

            String insertPlayerSQLQuery = "INSERT IGNORE INTO `Players`(playerId, kills, deaths, assists, firstBloodKill, firstBloodAssist, firstBloodTowerKill, firstBloodTowerAssist, summonerId, champPlayed) VALUES ( '"
                    + playerId + "', '" + kills + "', '" + deaths + "','" + assists + "', '" + firstBloodKill + "', '"
                    + firstBloodAssist + "', '" + firstBloodTowerKill + "', '" + firstBloodTowerAssist + "', '" + summonerId + "', '" + champPlayed + "')";

            //System.out.println(insertPlayerSQLQuery);

            Statement stmt = posConnection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);

            posConnection.setAutoCommit(false);

            stmt.addBatch(insertPlayerSQLQuery);

            stmt.executeBatch();

            posConnection.commit();

            stmt.clearBatch();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    
    public static void insertMatch(){
        
    }

    public static Summoner insertSummoner() throws RiotApiException {
        Scanner scan = new Scanner(System.in);

        System.out.println("Enter the summoner you'd like to retrieve: ");
        String input = scan.nextLine();
        System.out.println(input);
        input.replaceAll("\\s+", "");

        System.out.println(input);
        
        Summoner summoner;
        
        Connection posConnection = dbConnect();

        try {

            DatabaseMetaData dbmd = posConnection.getMetaData();
            
        } catch (SQLException e) {
            e.printStackTrace();
        }

        ApiConfig config = new ApiConfig().setKey(insertAPI());
        RiotApi api = new RiotApi(config);

        try {

            summoner = api.getSummonerByName(Platform.NA, input);
            
            long accountId = summoner.getAccountId();           

            String name = summoner.getName();

            String sql = "INSERT INTO `AllSummoners`(summonerId, summonerName) "
                    + "SELECT * FROM (SELECT '" + accountId + "', '" + name + "')"
                    + " AS temp WHERE NOT EXISTS "
                    + "(SELECT summonerId FROM `AllSummoners` WHERE summonerId = '" + accountId + "') LIMIT 1";

            Statement stmt = posConnection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);

            posConnection.setAutoCommit(false);

            stmt.addBatch(sql);

            stmt.executeBatch();

            posConnection.commit();

            stmt.clearBatch();
            
            return summoner;
        }
        
        catch(Exception e){
            e.printStackTrace();
        }

        return null;
        
    }

    public static String insertAPI() throws RiotApiException{
        
        return "RGAPI-cf959135-0593-405a-846c-07419bbb67eb";
        
    }
    
    
    public static void getMatches() throws RiotApiException {

        Connection posConnection = dbConnect();
        
        try {          
            posConnection.setAutoCommit(false);
            
            Summoner summoner = insertSummoner();
            
            ApiConfig config = new ApiConfig().setKey(insertAPI());
            
            RiotApi api = new RiotApi(config);
            
            MatchList matchList = api.getMatchListByAccountId(Platform.NA, summoner.getAccountId());
            
            Statement stmt = posConnection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
            
            ResultSet rs;

            posConnection.commit();

            int check = 0;

            stmt.clearBatch();

            if (matchList.getMatches() != null) {
                for (MatchReference match : matchList.getMatches()) {
                    if (check < 100 && match.getQueue() == 4) {

                        try {
                            Thread.sleep(10000);
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                        }

                        Match thisMatch = api.getMatch(Platform.NA, match.getGameId());
                        String matchStr = String.valueOf(thisMatch.getGameId());
                        
                        List<Participant> participants = thisMatch.getParticipants();

                        List<ParticipantIdentity> partID = thisMatch.getParticipantIdentities();

                        List<TeamStats> teamStats = thisMatch.getTeams();

                        Iterator<TeamStats> lsIt = teamStats.listIterator();

                        TeamStats team1Stats = lsIt.next();

                        TeamStats team2Stats = lsIt.next();

                        int team1Id = team1Stats.getTeamId();

                        int team2Id = team2Stats.getTeamId();

                        Map<String, String> acctIDToPartIdMap = new HashMap<String, String>();

                        for (ParticipantIdentity parId : partID) {
                            acctIDToPartIdMap.put(String.valueOf(parId.getParticipantId()), String.valueOf(parId.getPlayer().getAccountId()));
                        }

                        ParticipantTimeline midTime;
                        ParticipantTimeline jngTime;
                        ParticipantTimeline topTime;
                        ParticipantTimeline adcTime;
                        ParticipantTimeline supTime;

                        ParticipantTimeline eMidTime;
                        ParticipantTimeline eTopTime;
                        ParticipantTimeline eJngTime;
                        ParticipantTimeline eSupTime;
                        ParticipantTimeline eAdcTime;

                        String mid1PIdS = matchStr + "1" + "3";
                        String top1PIdS = matchStr + "1" + "1";
                        String jng1PIdS = matchStr + "1" + "2";
                        String adc1PIdS = matchStr + "1" + "4";
                        String sup1PIdS = matchStr + "1" + "5";

                        String mid2PIdS = matchStr + "2" + "3";
                        String top2PIdS = matchStr + "2" + "1";
                        String jng2PIdS = matchStr + "2" + "2";
                        String adc2PIdS = matchStr + "2" + "4";
                        String sup2PIdS = matchStr + "2" + "5";

                        BigInteger mid1PId = new BigInteger(mid1PIdS);
                        BigInteger top1PId = new BigInteger(top1PIdS);
                        BigInteger jng1PId = new BigInteger(jng1PIdS);
                        BigInteger adc1PId = new BigInteger(adc1PIdS);
                        BigInteger sup1PId = new BigInteger(sup1PIdS);

                        BigInteger mid2PId = new BigInteger(mid2PIdS);
                        BigInteger top2PId = new BigInteger(top2PIdS);
                        BigInteger jng2PId = new BigInteger(jng2PIdS);
                        BigInteger adc2PId = new BigInteger(adc2PIdS);
                        BigInteger sup2PId = new BigInteger(sup2PIdS);

                        for (Participant part : participants) {
                            switch (part.getTimeline().getLane().toLowerCase()) {
                                case "mid":
                                case "middle":
                                    if (part.getTeamId() == team1Id) {
                                        midTime = part.getTimeline();
                                        insertPlayers(posConnection, matchStr, mid1PId, part.getStats().getKills(), part.getStats().getDeaths(),
                                                part.getStats().getAssists(), String.valueOf(part.getStats().isFirstBloodKill()), String.valueOf(part.getStats().isFirstBloodAssist()),
                                                String.valueOf(part.getStats().isFirstTowerKill()), String.valueOf(part.getStats().isFirstTowerAssist()),
                                                acctIDToPartIdMap.get(String.valueOf(part.getParticipantId())), convertChamp(part.getChampionId()));
                                    } else if (part.getTeamId() == team2Id) {
                                        midTime = part.getTimeline();
                                        insertPlayers(posConnection, matchStr, mid2PId, part.getStats().getKills(), part.getStats().getDeaths(),
                                                part.getStats().getAssists(), String.valueOf(part.getStats().isFirstBloodKill()), String.valueOf(part.getStats().isFirstBloodAssist()),
                                                String.valueOf(part.getStats().isFirstTowerKill()), String.valueOf(part.getStats().isFirstTowerAssist()),
                                                acctIDToPartIdMap.get(String.valueOf(part.getParticipantId())), convertChamp(part.getChampionId()));
                                    }
                                    break;
                                case "jungle":
                                    if (part.getTeamId() == team1Id) {
                                        jngTime = part.getTimeline();
                                        insertPlayers(posConnection, matchStr, jng1PId, part.getStats().getKills(), part.getStats().getDeaths(),
                                                part.getStats().getAssists(), String.valueOf(part.getStats().isFirstBloodKill()), String.valueOf(part.getStats().isFirstBloodAssist()),
                                                String.valueOf(part.getStats().isFirstTowerKill()), String.valueOf(part.getStats().isFirstTowerAssist()),
                                                acctIDToPartIdMap.get(String.valueOf(part.getParticipantId())), convertChamp(part.getChampionId()));
                                    } else if (part.getTeamId() == team2Id) {
                                        eJngTime = part.getTimeline();
                                        insertPlayers(posConnection, matchStr, jng2PId, part.getStats().getKills(), part.getStats().getDeaths(),
                                                part.getStats().getAssists(), String.valueOf(part.getStats().isFirstBloodKill()), String.valueOf(part.getStats().isFirstBloodAssist()),
                                                String.valueOf(part.getStats().isFirstTowerKill()), String.valueOf(part.getStats().isFirstTowerAssist()),
                                                acctIDToPartIdMap.get(String.valueOf(part.getParticipantId())), convertChamp(part.getChampionId()));
                                    }
                                    break;

                                case "top":
                                    if (part.getTeamId() == team1Id) {
                                        topTime = part.getTimeline();
                                        insertPlayers(posConnection, matchStr, top1PId, part.getStats().getKills(), part.getStats().getDeaths(),
                                                part.getStats().getAssists(), String.valueOf(part.getStats().isFirstBloodKill()), String.valueOf(part.getStats().isFirstBloodAssist()),
                                                String.valueOf(part.getStats().isFirstTowerKill()), String.valueOf(part.getStats().isFirstTowerAssist()),
                                                acctIDToPartIdMap.get(String.valueOf(part.getParticipantId())), convertChamp(part.getChampionId()));
                                    } else if (part.getTeamId() == team2Id) {
                                        eTopTime = part.getTimeline();
                                        insertPlayers(posConnection, matchStr, top2PId, part.getStats().getKills(), part.getStats().getDeaths(),
                                                part.getStats().getAssists(), String.valueOf(part.getStats().isFirstBloodKill()), String.valueOf(part.getStats().isFirstBloodAssist()),
                                                String.valueOf(part.getStats().isFirstTowerKill()), String.valueOf(part.getStats().isFirstTowerAssist()),
                                                acctIDToPartIdMap.get(String.valueOf(part.getParticipantId())), convertChamp(part.getChampionId()));
                                    }
                                    break;
                                case "bottom":
                                case "bot":
                                    switch (part.getTimeline().getRole().toLowerCase()) {
                                        case "duo_support":
                                            if (part.getTeamId() == team1Id) {
                                                supTime = part.getTimeline();
                                                insertPlayers(posConnection, matchStr, sup1PId, part.getStats().getKills(), part.getStats().getDeaths(),
                                                        part.getStats().getAssists(), String.valueOf(part.getStats().isFirstBloodKill()), String.valueOf(part.getStats().isFirstBloodAssist()),
                                                        String.valueOf(part.getStats().isFirstTowerKill()), String.valueOf(part.getStats().isFirstTowerAssist()),
                                                        acctIDToPartIdMap.get(String.valueOf(part.getParticipantId())), convertChamp(part.getChampionId()));
                                            } else if (part.getTeamId() == team2Id) {
                                                eSupTime = part.getTimeline();
                                                insertPlayers(posConnection, matchStr, sup2PId, part.getStats().getKills(), part.getStats().getDeaths(),
                                                        part.getStats().getAssists(), String.valueOf(part.getStats().isFirstBloodKill()), String.valueOf(part.getStats().isFirstBloodAssist()),
                                                        String.valueOf(part.getStats().isFirstTowerKill()), String.valueOf(part.getStats().isFirstTowerAssist()),
                                                        acctIDToPartIdMap.get(String.valueOf(part.getParticipantId())), convertChamp(part.getChampionId()));
                                            }
                                            break;
                                        case "duo_carry":
                                            if (part.getTeamId() == team1Id) {
                                                adcTime = part.getTimeline();
                                                insertPlayers(posConnection, matchStr, adc1PId, part.getStats().getKills(), part.getStats().getDeaths(),
                                                        part.getStats().getAssists(), String.valueOf(part.getStats().isFirstBloodKill()), String.valueOf(part.getStats().isFirstBloodAssist()),
                                                        String.valueOf(part.getStats().isFirstTowerKill()), String.valueOf(part.getStats().isFirstTowerAssist()),
                                                        acctIDToPartIdMap.get(String.valueOf(part.getParticipantId())), convertChamp(part.getChampionId()));
                                            } else if (part.getTeamId() == team2Id) {
                                                eAdcTime = part.getTimeline();
                                                insertPlayers(posConnection, matchStr, adc2PId, part.getStats().getKills(), part.getStats().getDeaths(),
                                                        part.getStats().getAssists(), String.valueOf(part.getStats().isFirstBloodKill()), String.valueOf(part.getStats().isFirstBloodAssist()),
                                                        String.valueOf(part.getStats().isFirstTowerKill()), String.valueOf(part.getStats().isFirstTowerAssist()),
                                                        acctIDToPartIdMap.get(String.valueOf(part.getParticipantId())), convertChamp(part.getChampionId()));
                                            }
                                            break;
                                    }
                                    break;

                            }

                        }

                        String role = match.getRole();
                        Date expiry = new Date(thisMatch.getGameCreation());
                        
                        String team1FB = String.valueOf(team1Stats.isFirstBlood());
                        String team1FBar = String.valueOf(team1Stats.isFirstBaron());
                        String team1FTower = String.valueOf(team1Stats.isFirstTower());
                        String team1FDrag = String.valueOf(team1Stats.isFirstDragon());

                        String team2FB = String.valueOf(team2Stats.isFirstBlood());
                        String team2FBar = String.valueOf(team2Stats.isFirstBaron());
                        String team2FTower = String.valueOf(team2Stats.isFirstTower());
                        String team2FDrag = String.valueOf(team2Stats.isFirstDragon());

                        long duration = thisMatch.getGameDuration();
                        int minutes;
                        int seconds;
                        minutes = (int) duration / 60;
                        double remainder = duration % 60;
                        seconds = (int) Math.round(remainder * 60);                        
                        String secondsStr = String.valueOf(seconds);                        
                        secondsStr.substring(0, 1);                   

                        
                       String matchSQLQuery = "INSERT INTO `AllMatches`(matchId, team1Id, team2Id, date, gameDuration) "
                    + "SELECT * FROM (SELECT '" + matchStr + "', '" + matchStr + "1', '" + matchStr + "2', '" + expiry.toString() + "', '" + minutes + ":" + secondsStr + "')"
                    + " AS temp WHERE NOT EXISTS "
                    + "(SELECT matchId FROM `AllMatches` WHERE matchId = '" + matchStr + "') LIMIT 1";
                       
                        System.out.println(matchSQLQuery);
                        
                        stmt = posConnection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);

                        posConnection.setAutoCommit(false);

                        stmt.addBatch(matchSQLQuery);

                        stmt.executeBatch();

                        posConnection.commit();

                        stmt.clearBatch();                        
                        

                        String team1SQLQuery = "INSERT IGNORE INTO `Teams`(midPlayerId, topPlayerId, jngPlayerId, supPlayerId, adcPlayerId, teamFirstBlood,"
                                + " teamFirstBaron, teamFirstTower, teamFirstDrag, isWin, teamId) VALUES ('"
                                + mid1PId + "', '" + top1PId + "', '" + jng1PId + "','" + sup1PId + "', '" + adc1PId + "', '"
                                + team1FB + "', '" + team1FBar + "', '" + team1FTower + "', '" + team1FDrag + "', '" + team1Stats.getWin() + "','" + matchStr + "1')";
                        
                        

                        stmt = posConnection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);

                        posConnection.setAutoCommit(false);

                        stmt.addBatch(team1SQLQuery);

                        stmt.executeBatch();

                        posConnection.commit();

                        stmt.clearBatch();      

                        String team2SQLQuery = "REPLACE INTO `Teams`(midPlayerId, topPlayerId, jngPlayerId, supPlayerId, adcPlayerId, teamFirstBlood, "
                                + "teamFirstBaron, teamFirstTower, teamFirstDrag, isWin, teamId) VALUES('"
                                + mid2PId + "', '" + top2PId + "', '" + jng2PId + "','" + sup2PId + "', '" + adc2PId + "', '"
                                + team2FB + "', '" + team2FBar + "', '" + team2FTower + "', '" + team2FDrag + "', '" + team2Stats.getWin() + "','" + matchStr + "2')";

                        stmt = posConnection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);

                        posConnection.setAutoCommit(false);

                        stmt.addBatch(team2SQLQuery);

                        stmt.executeBatch();

                        posConnection.commit();

                        stmt.clearBatch();
                        
                        String matchToTeamSQL = "insert ignore into `matchToTeam` (teamId, matchId, enemyTeamId) select team1Id, matchId, team2Id from `AllMatches`;";

                        stmt.addBatch(matchToTeamSQL);

                        stmt.executeBatch();

                        posConnection.commit();

                        stmt.clearBatch();
                        
                        String midplayerToTeamSQL = "insert ignore into `PlayerToTeam`(select playerId, teamId from `Teams` left join `Players` on Players.playerId = Teams.midPlayerId);";
                        
                        stmt.addBatch(midplayerToTeamSQL);

                        stmt.executeBatch();

                        posConnection.commit();

                        stmt.clearBatch();
                        
                        String adcplayerToTeamSQL = "insert ignore into `PlayerToTeam`(select playerId, teamId from `Teams` left join `Players` on Players.playerId = Teams.adcPlayerId);";
                        
                        stmt.addBatch(adcplayerToTeamSQL);

                        stmt.executeBatch();

                        posConnection.commit();

                        stmt.clearBatch();
                        
                        String jngplayerToTeamSQL = "insert ignore into `PlayerToTeam`(select playerId, teamId from `Teams` left join `Players` on Players.playerId = Teams.jngPlayerId);";
                        
                        stmt.addBatch(jngplayerToTeamSQL);

                        stmt.executeBatch();

                        posConnection.commit();

                        stmt.clearBatch();
                        
                        String topplayerToTeamSQL = "insert ignore into `PlayerToTeam`(select playerId, teamId from `Teams` left join `Players` on Players.playerId = Teams.topPlayerId);";
                        
                        stmt.addBatch(topplayerToTeamSQL);

                        stmt.executeBatch();

                        posConnection.commit();

                        stmt.clearBatch();
                        
                        String supplayerToTeamSQL = "insert ignore into `PlayerToTeam`(select playerId, teamId from `Teams` left join `Players` on Players.playerId = Teams.supPlayerId);";
                        
                        stmt.addBatch(supplayerToTeamSQL);

                        stmt.executeBatch();

                        posConnection.commit();

                        stmt.clearBatch();
                                                
                        check++;
                    }
                }
            }

            System.out.println("Record saved!");

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static String convertChamp(int champId) {

        String champName = "";

        switch (champId) {
            case 24:
                champName = "Jax";
                break;
            case 37:
                champName = "Sona";
                break;
            case 18:
                champName = "Tristana";
                break;
            case 110:
                champName = "Varus";
                break;
            case 114:
                champName = "Fiora";
                break;
            case 27:
                champName = "Singed";
                break;
            case 223:
                champName = "TahmKench";
                break;
            case 7:
                champName = "Leblanc";
                break;
            case 412:
                champName = "Thresh";
                break;
            case 43:
                champName = "Karma";
                break;
            case 202:
                champName = "Jhin";
                break;
            case 68:
                champName = "Rumble";
                break;
            case 77:
                champName = "Udyr";
                break;
            case 64:
                champName = "LeeSin";
                break;
            case 83:
                champName = "Yorick";
                break;
            case 141:
                champName = "Kayn";
                break;
            case 38:
                champName = "Kassadin";
                break;
            case 15:
                champName = "Sivir";
                break;
            case 21:
                champName = "MissFortune";
                break;
            case 119:
                champName = "Draven";
                break;
            case 157:
                champName = "Yasuo";
                break;
            case 10:
                champName = "Kayle";
                break;
            case 35:
                champName = "Shaco";
                break;
            case 58:
                champName = "Renekton";
                break;
            case 120:
                champName = "Hecarim";
                break;
            case 105:
                champName = "Fizz";
                break;
            case 96:
                champName = "KogMaw";
                break;
            case 57:
                champName = "Maokai";
                break;
            case 127:
                champName = "Lissandra";
                break;
            case 222:
                champName = "Jinx";
                break;
            case 6:
                champName = "Urgot";
                break;
            case 9:
                champName = "Fiddlesticks";
                break;
            case 3:
                champName = "Galio";
                break;
            case 80:
                champName = "Pantheon";
                break;
            case 91:
                champName = "Talon";
                break;
            case 41:
                champName = "Gangplank";
                break;
            case 81:
                champName = "Ezreal";
                break;
            case 150:
                champName = "Gnar";
                break;
            case 17:
                champName = "Teemo";
                break;
            case 1:
                champName = "Annie";
                break;
            case 82:
                champName = "Mordekaiser";
                break;
            case 268:
                champName = "Azir";
                break;
            case 85:
                champName = "Kennen";
                break;
            case 92:
                champName = "Riven";
                break;
            case 31:
                champName = "Chogath";
                break;
            case 266:
                champName = "Aatrox";
                break;
            case 78:
                champName = "Poppy";
                break;
            case 163:
                champName = "Taliyah";
                break;
            case 420:
                champName = "Illaoi";
                break;
            case 74:
                champName = "Heimerdinger";
                break;
            case 12:
                champName = "Alistar";
                break;
            case 5:
                champName = "XinZhao";
                break;
            case 236:
                champName = "Lucian";
                break;
            case 106:
                champName = "Volibear";
                break;
            case 113:
                champName = "Sejuani";
                break;
            case 76:
                champName = "Nidalee";
                break;
            case 86:
                champName = "Garen";
                break;
            case 89:
                champName = "Leona";
                break;
            case 238:
                champName = "Zed";
                break;
            case 53:
                champName = "Blitzcrank";
                break;
            case 33:
                champName = "Rammus";
                break;
            case 161:
                champName = "Velkoz";
                break;
            case 51:
                champName = "Caitlyn";
                break;
            case 48:
                champName = "Trundle";
                break;
            case 203:
                champName = "Kindred";
                break;
            case 133:
                champName = "Quinn";
                break;
            case 245:
                champName = "Ekko";
                break;
            case 267:
                champName = "Nami";
                break;
            case 50:
                champName = "Swain";
                break;
            case 44:
                champName = "Taric";
                break;
            case 134:
                champName = "Syndra";
                break;
            case 497:
                champName = "Rakan";
                break;
            case 72:
                champName = "Skarner";
                break;
            case 201:
                champName = "Braum";
                break;
            case 45:
                champName = "Veigar";
                break;
            case 101:
                champName = "Xerath";
                break;
            case 42:
                champName = "Corki";
                break;
            case 111:
                champName = "Nautilus";
                break;
            case 103:
                champName = "Ahri";
                break;
            case 126:
                champName = "Jayce";
                break;
            case 122:
                champName = "Darius";
                break;
            case 23:
                champName = "Tryndamere";
                break;
            case 40:
                champName = "Janna";
                break;
            case 60:
                champName = "Elise";
                break;
            case 67:
                champName = "Vayne";
                break;
            case 63:
                champName = "Brand";
                break;
            case 104:
                champName = "Graves";
                break;
            case 16:
                champName = "Soraka";
                break;
            case 498:
                champName = "Xayah";
                break;
            case 30:
                champName = "Karthus";
                break;
            case 8:
                champName = "Vladimir";
                break;
            case 26:
                champName = "Zilean";
                break;
            case 55:
                champName = "Katarina";
                break;
            case 102:
                champName = "Shyvana";
                break;
            case 19:
                champName = "Warwick";
                break;
            case 115:
                champName = "Ziggs";
                break;
            case 240:
                champName = "Kled";
                break;
            case 121:
                champName = "Khazix";
                break;
            case 2:
                champName = "Olaf";
                break;
            case 4:
                champName = "TwistedFate";
                break;
            case 20:
                champName = "Nunu";
                break;
            case 107:
                champName = "Rengar";
                break;
            case 432:
                champName = "Bard";
                break;
            case 39:
                champName = "Irelia";
                break;
            case 427:
                champName = "Ivern";
                break;
            case 62:
                champName = "MonkeyKing";
                break;
            case 22:
                champName = "Ashe";
                break;
            case 429:
                champName = "Kalista";
                break;
            case 84:
                champName = "Akali";
                break;
            case 254:
                champName = "Vi";
                break;
            case 32:
                champName = "Amumu";
                break;
            case 117:
                champName = "Lulu";
                break;
            case 25:
                champName = "Morgana";
                break;
            case 56:
                champName = "Nocturne";
                break;
            case 131:
                champName = "Diana";
                break;
            case 136:
                champName = "AurelionSol";
                break;
            case 143:
                champName = "Zyra";
                break;
            case 112:
                champName = "Viktor";
                break;
            case 69:
                champName = "Cassiopeia";
                break;
            case 75:
                champName = "Nasus";
                break;
            case 29:
                champName = "Twitch";
                break;
            case 36:
                champName = "DrMundo";
                break;
            case 61:
                champName = "Orianna";
                break;
            case 28:
                champName = "Evelynn";
                break;
            case 421:
                champName = "RekSai";
                break;
            case 99:
                champName = "Lux";
                break;
            case 14:
                champName = "Sion";
                break;
            case 164:
                champName = "Camille";
                break;
            case 11:
                champName = "MasterYi";
                break;
            case 13:
                champName = "Ryze";
                break;
            case 54:
                champName = "Malphite";
                break;
            case 34:
                champName = "Anivia";
                break;
            case 98:
                champName = "Shen";
                break;
            case 59:
                champName = "JarvanIV";
                break;
            case 90:
                champName = "Malzahar";
                break;
            case 154:
                champName = "Zac";
                break;
            case 79:
                champName = "Gragas";
                break;
        }

        return champName;
    }

}
